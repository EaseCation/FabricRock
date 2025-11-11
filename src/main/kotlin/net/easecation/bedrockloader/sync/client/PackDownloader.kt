package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.common.MD5Util
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Resource Pack Downloader
 * Downloads resource pack files from HTTP server to remote/ subdirectory
 */
class PackDownloader(
    private val config: ClientConfig,
    private val packDirectory: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PackDownloader")

    /**
     * Remote pack storage directory (packDirectory/remote)
     */
    private val remoteDirectory = File(packDirectory, "remote").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
        .build()

    /**
     * Download single file result
     */
    sealed class DownloadFileResult {
        data class Success(val file: File) : DownloadFileResult()
        data class Failed(val error: SyncError) : DownloadFileResult()
    }

    /**
     * Batch download result
     */
    data class BatchDownloadResult(
        val successFiles: List<String>,
        val failedFiles: List<String>,
        val successCount: Int,
        val failCount: Int
    )

    /**
     * Cleanup temporary files in remote/ directory
     * Removes all .downloading and .backup suffix files
     */
    fun cleanupTempFiles() {
        var cleanedCount = 0

        remoteDirectory.listFiles()?.forEach { file ->
            if (file.name.endsWith(".downloading") || file.name.endsWith(".backup")) {
                if (file.delete()) {
                    cleanedCount++
                } else {
                    logger.warn("Cannot delete temp file: ${file.name}")
                }
            }
        }

        if (cleanedCount > 0) {
            logger.info("Cleaned up $cleanedCount temp file(s)")
        }
    }

    /**
     * Download single file (with retry mechanism)
     *
     * @param pack Resource pack info
     * @param onProgress Progress callback (bytesDownloaded, totalBytes)
     * @param isCancelled Function to check if cancelled
     * @param maxRetries Maximum retry attempts
     * @return Download result
     */
    fun downloadFile(
        pack: RemotePackInfo,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean,
        maxRetries: Int = 3
    ): DownloadFileResult {
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                // 1. Download to temp file
                val tempFile = downloadToTempFile(pack, onProgress, isCancelled)

                // 2. Verify MD5 and replace
                verifyMD5AndReplace(tempFile, pack)

                return DownloadFileResult.Success(File(remoteDirectory, pack.name))

            } catch (e: CancelledException) {
                // User cancelled, don't retry
                logger.warn("Download cancelled: ${pack.name}")
                throw e
            } catch (e: Exception) {
                lastError = e
                logger.warn("Download failed (attempt ${attempt + 1}/$maxRetries): ${pack.name} - ${e.message}")

                if (attempt < maxRetries - 1) {
                    Thread.sleep(1000)
                }
            }
        }

        // All retries failed
        val errorMessage = "Download failed after $maxRetries retries: ${lastError?.message}"
        logger.error(errorMessage)
        return DownloadFileResult.Failed(
            SyncError.FileError(pack.name, errorMessage, lastError)
        )
    }

    /**
     * Batch download all files
     *
     * @param packs List of resource packs to download
     * @param onProgress Overall progress callback (currentIndex, total)
     * @param onFileStart File download start callback (file, index, total)
     * @param onFileProgress Single file progress callback (file, bytesDownloaded, totalBytes)
     * @param onFileComplete Single file complete callback (file)
     * @param isCancelled Function to check if cancelled
     * @return Batch download result
     */
    fun downloadAll(
        packs: List<RemotePackInfo>,
        onProgress: (Int, Int) -> Unit,
        onFileStart: (RemotePackInfo, Int, Int) -> Unit,
        onFileProgress: (RemotePackInfo, Long, Long) -> Unit,
        onFileComplete: (RemotePackInfo) -> Unit,
        isCancelled: () -> Boolean
    ): BatchDownloadResult {
        val successFiles = mutableListOf<String>()
        val failedFiles = mutableListOf<String>()

        packs.forEachIndexed { index, pack ->
            // Check cancelled status
            if (isCancelled()) {
                logger.warn("Batch download cancelled")
                throw CancelledException("User cancelled download")
            }

            // Notify overall progress
            onProgress(index + 1, packs.size)

            // Notify file download start
            onFileStart(pack, index + 1, packs.size)

            // Download single file
            val result = downloadFile(
                pack = pack,
                onProgress = { downloaded, total ->
                    onFileProgress(pack, downloaded, total)
                },
                isCancelled = isCancelled
            )

            // Process result
            when (result) {
                is DownloadFileResult.Success -> {
                    successFiles.add(pack.name)
                    onFileComplete(pack)
                }
                is DownloadFileResult.Failed -> {
                    failedFiles.add(pack.name)

                    // If configured to auto-cancel on error, throw exception
                    if (config.autoCancelOnError) {
                        throw result.error
                    }
                }
            }
        }

        val batchResult = BatchDownloadResult(
            successFiles = successFiles,
            failedFiles = failedFiles,
            successCount = successFiles.size,
            failCount = failedFiles.size
        )

        return batchResult
    }

    /**
     * Download file to temp file
     *
     * @param pack Resource pack info
     * @param onProgress Progress callback
     * @param isCancelled Function to check if cancelled
     * @return Downloaded temp file
     */
    private fun downloadToTempFile(
        pack: RemotePackInfo,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        // Build download URL
        val downloadUrl = "${config.serverUrl.trimEnd('/')}${pack.url}"

        // Create temp file in remote/ directory
        val tempFile = File(remoteDirectory, "${pack.name}.downloading")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            // Create HTTP request
            val request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                .build()

            // Send request and get response
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

            // Check HTTP status code
            when (response.statusCode()) {
                200 -> {
                    // Stream write to file
                    FileOutputStream(tempFile).use { output ->
                        response.body().use { input ->
                            val buffer = ByteArray(8192) // 8KB buffer
                            var totalBytesRead = 0L
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                // Check cancel status
                                if (isCancelled()) {
                                    tempFile.delete()
                                    throw CancelledException("User cancelled download")
                                }

                                // Write data
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                // Callback progress
                                onProgress(totalBytesRead, pack.size)
                            }
                        }
                    }

                    return tempFile
                }
                404 -> {
                    throw SyncError.ServerError(404, "File not found: ${pack.url}")
                }
                else -> {
                    throw SyncError.ServerError(response.statusCode(), "Server error: HTTP ${response.statusCode()}")
                }
            }
        } catch (e: ConnectException) {
            tempFile.delete()
            throw SyncError.NetworkError("Cannot connect to server: ${config.serverUrl}", e)
        } catch (e: java.net.SocketTimeoutException) {
            tempFile.delete()
            throw SyncError.NetworkError("Download timeout (${config.timeoutSeconds}s)", e)
        } catch (e: SyncError) {
            tempFile.delete()
            throw e
        } catch (e: CancelledException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            throw SyncError.FileError(pack.name, "Download failed: ${e.message}", e)
        }
    }

    /**
     * Verify MD5 and replace file
     *
     * @param tempFile Temp file
     * @param pack Resource pack info
     */
    private fun verifyMD5AndReplace(tempFile: File, pack: RemotePackInfo) {
        // Calculate file MD5
        val actualMD5 = try {
            MD5Util.calculateMD5(tempFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw SyncError.FileError(pack.name, "MD5 calculation failed: ${e.message}", e)
        }

        // Verify MD5 match
        if (!actualMD5.equals(pack.md5, ignoreCase = true)) {
            logger.error("MD5 verification failed - expected: ${pack.md5}, actual: $actualMD5")
            tempFile.delete()
            throw SyncError.FileError(
                pack.name,
                "MD5 verification failed (expected: ${pack.md5}, actual: $actualMD5)"
            )
        }

        // Atomic file replacement (in remote/ directory)
        val targetFile = File(remoteDirectory, pack.name)
        atomicReplace(targetFile, tempFile)
    }

    /**
     * Atomic file replacement
     * Uses backup and recovery mechanism to ensure atomicity
     *
     * @param targetFile Target file (in remote/ directory)
     * @param newFile New file (temp file)
     */
    private fun atomicReplace(targetFile: File, newFile: File) {
        val backup = File(remoteDirectory, "${targetFile.name}.backup")

        try {
            // 1. Backup old file if exists
            if (targetFile.exists()) {
                if (!targetFile.renameTo(backup)) {
                    throw IOException("Cannot backup old file")
                }
            }

            // 2. Move new file to target location
            if (!newFile.renameTo(targetFile)) {
                // Replacement failed, restore backup
                if (backup.exists()) {
                    backup.renameTo(targetFile)
                }
                throw IOException("Cannot replace file")
            }

            // 3. Delete backup
            if (backup.exists()) {
                backup.delete()
            }

        } catch (e: Exception) {
            // Restore backup on failure
            if (backup.exists() && !targetFile.exists()) {
                logger.warn("Replacement failed, restoring backup...")
                backup.renameTo(targetFile)
            }
            throw SyncError.FileError(targetFile.name, "File replacement failed: ${e.message}", e)
        }
    }

    /**
     * Format bytes to human-readable format
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

/**
 * Cancelled Exception
 * Indicates that the download was cancelled by the user
 */
class CancelledException(message: String = "Operation cancelled") : Exception(message)
