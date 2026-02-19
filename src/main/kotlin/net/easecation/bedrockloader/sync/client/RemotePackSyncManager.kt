package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.loader.InMemoryPackStore
import net.easecation.bedrockloader.loader.InMemoryZipPack
import net.easecation.bedrockloader.sync.client.listener.SyncListener
import net.easecation.bedrockloader.sync.client.model.*
import net.easecation.bedrockloader.sync.common.PackEncryption
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Remote Pack Sync Manager
 * Core class that coordinates all sync components
 */
class RemotePackSyncManager(
    private val packDirectory: File,
    private val configFile: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/RemoteSync")
    private val listeners = CopyOnWriteArrayList<SyncListener>()

    @Volatile
    private var currentState: SyncState = SyncState.Idle

    @Volatile
    private var cancelled = false

    // ==================== Listener Management ====================

    /**
     * Add sync listener
     */
    fun addListener(listener: SyncListener) {
        listeners.add(listener)
    }

    /**
     * Remove sync listener
     */
    fun removeListener(listener: SyncListener) {
        listeners.remove(listener)
    }

    /**
     * Clear all listeners
     */
    fun clearListeners() {
        listeners.clear()
    }

    // ==================== Core Sync Methods ====================

    /**
     * Check for pack updates
     *
     * @return Sync check result
     */
    fun checkForUpdates(): SyncResult {
        logger.info("Checking for pack updates...")
        setState(SyncState.LoadingConfig)
        notifyListeners { it.onCheckingStart() }

        try {
            // 1. Load config
            checkCancelled()
            notifyListeners { it.onCheckingProgress("Loading config...") }

            val config = ClientConfigLoader.loadClientConfig(configFile)

            if (!config.enabled) {
                logger.info("Remote sync disabled")
                setState(SyncState.Complete)
                return SyncResult.Disabled
            }

            logger.info("Server: ${config.serverUrl}, timeout: ${config.timeoutSeconds}s")

            // 2. Fetch remote manifest
            checkCancelled()
            setState(SyncState.FetchingManifest)
            notifyListeners { it.onCheckingProgress("Connecting to server: ${config.serverUrl}") }

            val client = ManifestClient(config)
            val manifest = client.fetchManifest()

            logger.info("Manifest fetched: ${manifest.packs.size} pack(s)")

            // 3. Compare local packs
            checkCancelled()
            setState(SyncState.ComparingPacks)
            notifyListeners { it.onCheckingProgress("Comparing local packs...") }

            val comparator = PackComparator(packDirectory, config)
            val plan = comparator.compare(manifest)

            logger.info("Comparison complete: ${plan.toDownload.size} new, ${plan.toUpdate.size} updates, ${plan.upToDate.size} up-to-date")

            if (plan.hasDownloads) {
                logger.info("Total to download: ${plan.totalToDownload} file(s), ${formatBytes(plan.totalBytes)}")
            }

            // 4. Return result
            setState(SyncState.PlanReady(plan))
            notifyListeners { it.onCheckingComplete(plan) }

            return SyncResult.Success(plan)

        } catch (e: CancelledException) {
            logger.warn("Check cancelled")
            setState(SyncState.Cancelled)
            notifyListeners { it.onCancelled("User cancelled check") }
            return SyncResult.Cancelled

        } catch (e: SyncError.NetworkError) {
            logger.warn("Network error: ${e.message}")
            val offline = SyncResult.Offline("Cannot connect to server, will use local packs")
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return offline

        } catch (e: SyncError) {
            logger.error("Sync error: ${e.message}", e.cause)
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return SyncResult.Failed(e)

        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            logger.error("Check failed: ${error.message}", e)
            setState(SyncState.Error(error))
            notifyListeners { it.onError(error) }
            return SyncResult.Failed(error)
        }
    }

    // ==================== Download Methods ====================

    /**
     * Download resource packs
     *
     * @param plan Sync plan
     * @return Download result
     */
    fun downloadPacks(plan: SyncPlan): DownloadResult {
        logger.info("Starting download...")

        // Check if there are files to download
        if (!plan.hasDownloads) {
            logger.info("No files to download")
            setState(SyncState.Complete)
            notifyListeners { it.onDownloadComplete(0, 0) }
            return DownloadResult.Success(emptyList(), emptyList())
        }

        // Set downloading state
        setState(SyncState.Downloading(0, plan.totalToDownload, 0f))

        try {
            // 1. Load config
            checkCancelled()
            val config = ClientConfigLoader.loadClientConfig(configFile)

            // 2. Create downloader
            val downloader = PackDownloader(config, packDirectory)

            // 3. Cleanup temp files
            downloader.cleanupTempFiles()

            // 4. Merge download list
            val packList = plan.toDownload + plan.toUpdate
            logger.info("Downloading ${packList.size} file(s), total size: ${formatBytes(plan.totalBytes)}")

            // 5. Notify download start
            notifyListeners { it.onDownloadStart(plan.totalToDownload, plan.totalBytes) }

            // 6. Execute batch download
            val result = downloader.downloadAll(
                packs = packList,
                onProgress = { index, total -> },
                onFileStart = { file, index, total ->
                    notifyListeners { it.onFileDownloadStart(file, index, total) }
                },
                onFileProgress = { file, bytesDownloaded, totalBytes ->
                    notifyListeners { it.onFileProgress(file, bytesDownloaded, totalBytes) }
                },
                onFileComplete = { file ->
                    notifyListeners { it.onFileDownloadComplete(file) }
                },
                isCancelled = { cancelled }
            )

            // 7. Cleanup removed packs
            if (plan.packagesToCleanup.isNotEmpty()) {
                cleanupRemovedPacks(plan.packagesToCleanup)
            }

            // 8. Process download result
            setState(SyncState.Complete)
            notifyListeners { it.onDownloadComplete(result.successCount, result.failCount) }

            logger.info("Download complete: ${result.successCount} succeeded, ${result.failCount} failed")

            return if (result.failCount == 0) {
                DownloadResult.Success(result.successFiles, emptyList())
            } else {
                DownloadResult.Success(result.successFiles, result.failedFiles)
            }

        } catch (e: CancelledException) {
            logger.warn("Download cancelled")
            setState(SyncState.Cancelled)
            notifyListeners { it.onCancelled("User cancelled download") }
            return DownloadResult.Cancelled

        } catch (e: SyncError) {
            logger.error("Download failed: ${e.message}", e.cause)
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return DownloadResult.Failed(e)

        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            logger.error("Download failed: ${error.message}", e)
            setState(SyncState.Error(error))
            notifyListeners { it.onError(error) }
            return DownloadResult.Failed(error)
        }
    }

    // ==================== Control Methods ====================

    /**
     * Cancel current sync operation
     */
    fun cancel() {
        if (!cancelled) {
            logger.info("Cancel requested")
            cancelled = true
        }
    }

    /**
     * Reset sync manager state
     */
    fun reset() {
        cancelled = false
        setState(SyncState.Idle)
    }

    /**
     * Get current state
     */
    fun getCurrentState(): SyncState = currentState

    /**
     * Is cancelled
     */
    fun isCancelled(): Boolean = cancelled

    // ==================== Internal Methods ====================

    /**
     * Set state
     */
    private fun setState(newState: SyncState) {
        currentState = newState
    }

    /**
     * Notify all listeners
     */
    private fun notifyListeners(action: (SyncListener) -> Unit) {
        listeners.forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                logger.error("Listener failed: ${listener.javaClass.simpleName}", e)
            }
        }
    }

    /**
     * Check if cancelled
     * @throws CancelledException if cancelled
     */
    private fun checkCancelled() {
        if (cancelled) {
            throw CancelledException("Operation cancelled")
        }
    }

    /**
     * Map exception to SyncError
     */
    private fun mapExceptionToError(e: Exception): SyncError {
        return when (e) {
            is SyncError -> e
            is ConnectException -> SyncError.NetworkError("Cannot connect to server", e)
            is java.net.SocketTimeoutException -> SyncError.NetworkError("Connection timeout", e)
            is IOException -> SyncError.NetworkError("Network IO error", e)
            else -> SyncError.UnknownError(e.message ?: "Unknown error", e)
        }
    }

    /**
     * 解密本地缓存的密文包到内存
     *
     * 流程:
     * 1. 从 manifest 中提取 server_token
     * 2. 构造 KeyExchangeClient，通过 Challenge-Response 握手获取每包密钥
     * 3. 从 remote/ 读取密文文件
     * 4. 解密到内存 -> 存入 InMemoryPackStore
     *
     * @param manifest 远程清单（包含加密配置和包列表）
     * @return 解密成功的包数量
     */
    fun decryptLocalPacksToMemory(manifest: RemotePackManifest): Int {
        val encryption = manifest.encryption ?: return 0
        if (!encryption.enabled) return 0

        logger.info("Starting pack decryption (${manifest.packs.size} pack(s))...")

        val config = ClientConfigLoader.loadClientConfig(configFile)
        val serverToken = encryption.serverToken
        val keyExchangeClient = KeyExchangeClient(config.serverUrl, serverToken, config.timeoutSeconds)

        val remoteDir = File(packDirectory, "remote")
        val encryptedPacks = manifest.packs.filter { it.encrypted }

        if (encryptedPacks.isEmpty()) {
            logger.info("No encrypted packs to decrypt")
            return 0
        }

        // 批量获取密钥
        val filenames = encryptedPacks.map { it.name }
        logger.info("Requesting decryption keys for ${filenames.size} pack(s)...")
        val keys = keyExchangeClient.fetchKeys(filenames)

        // 解密每个包到内存
        var decryptedCount = 0
        for (packInfo in encryptedPacks) {
            val file = File(remoteDir, packInfo.name)
            if (!file.exists()) {
                logger.warn("Encrypted pack file not found: ${packInfo.name}")
                continue
            }

            val key = keys[packInfo.name]
            if (key == null) {
                logger.error("No decryption key for: ${packInfo.name}")
                continue
            }

            try {
                logger.info("Decrypting: ${packInfo.name} (${formatBytes(file.length())})")
                val encryptedData = file.readBytes()
                val decryptedData = PackEncryption.decrypt(encryptedData, key)
                val memoryPack = InMemoryZipPack(decryptedData)
                InMemoryPackStore.store(packInfo.name, memoryPack)
                decryptedCount++
                logger.info("Decrypted to memory: ${packInfo.name} (${memoryPack.entryNames.size} entries)")
            } catch (e: Exception) {
                logger.error("Failed to decrypt: ${packInfo.name}", e)
            }
        }

        InMemoryPackStore.logMemoryUsage()
        logger.info("Decryption complete: $decryptedCount/${encryptedPacks.size} pack(s)")
        return decryptedCount
    }

    /**
     * Cleanup removed packs
     * Delete packs in remote/ directory that are removed from server
     *
     * @param filenames List of filenames to cleanup
     */
    private fun cleanupRemovedPacks(filenames: List<String>) {
        if (filenames.isEmpty()) {
            return
        }

        logger.info("Cleaning up ${filenames.size} removed pack(s)")
        val remoteDirectory = File(packDirectory, "remote")

        var cleanedCount = 0
        var failedCount = 0

        for (filename in filenames) {
            val file = File(remoteDirectory, filename)
            if (file.exists()) {
                if (file.delete()) {
                    cleanedCount++
                } else {
                    failedCount++
                    logger.warn("Failed to delete: $filename")
                }
            }
        }

        logger.info("Cleanup complete: $cleanedCount succeeded, $failedCount failed")
    }

    /**
     * Format bytes to human-readable format
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
