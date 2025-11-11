package net.easecation.bedrockloader.sync.client.listener

import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.client.model.SyncPlan
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import org.slf4j.LoggerFactory

/**
 * Console Sync Listener
 * Outputs sync progress to logs
 */
class ConsoleSyncListener : SyncListener {
    private val logger = LoggerFactory.getLogger("BedrockLoader/Sync")

    // ==================== Checking Phase ====================

    override fun onCheckingStart() {
        logger.info("Checking for pack updates...")
    }

    override fun onCheckingProgress(message: String) {
        logger.info(message)
    }

    override fun onCheckingComplete(plan: SyncPlan) {
        if (plan.isUpToDate) {
            logger.info("All packs are up to date (${plan.upToDate.size} pack(s))")
        } else {
            val messages = mutableListOf<String>()

            if (plan.toDownload.isNotEmpty()) {
                messages.add("${plan.toDownload.size} new pack(s)")
            }
            if (plan.toUpdate.isNotEmpty()) {
                messages.add("${plan.toUpdate.size} update(s)")
            }
            if (plan.upToDate.isNotEmpty()) {
                messages.add("${plan.upToDate.size} up-to-date")
            }

            logger.info("Found changes: ${messages.joinToString(", ")}")

            if (plan.hasDownloads) {
                logger.info("Total to download: ${plan.totalToDownload} file(s), ${formatBytes(plan.totalBytes)}")
            }
        }
    }

    // ==================== Download Phase ====================

    override fun onDownloadStart(totalFiles: Int, totalBytes: Long) {
        logger.info("Starting download: $totalFiles file(s), ${formatBytes(totalBytes)}")
    }

    override fun onFileDownloadStart(file: RemotePackInfo, index: Int, total: Int) {
        logger.info("[$index/$total] Downloading: ${file.name} (${formatBytes(file.size)})")
    }

    override fun onFileProgress(file: RemotePackInfo, bytesDownloaded: Long, totalBytes: Long) {
        // Reduce log spam - only log at debug level
    }

    override fun onFileDownloadComplete(file: RemotePackInfo) {
        logger.info("Completed: ${file.name}")
    }

    override fun onDownloadComplete(successCount: Int, failCount: Int) {
        if (failCount == 0) {
            logger.info("All packs downloaded successfully ($successCount file(s))")
        } else {
            logger.warn("Download completed with errors: $successCount succeeded, $failCount failed")
        }
    }

    // ==================== Error and Cancellation ====================

    override fun onError(error: SyncError) {
        logger.error("Sync failed: ${error.message}")

        when (error) {
            is SyncError.NetworkError -> {
                logger.error("Network error - please check:")
                logger.error("  - Server URL is correct")
                logger.error("  - Network connection is available")
                logger.error("  - Server is running")
            }
            is SyncError.ServerError -> {
                logger.error("Server error (HTTP ${error.statusCode})")
            }
            is SyncError.ConfigError -> {
                logger.error("Config error - please check configuration file")
            }
            is SyncError.FileError -> {
                logger.error("File operation failed: ${error.filename}")
            }
            is SyncError.UnknownError -> {
                if (error.cause != null) {
                    logger.error("Details:", error.cause)
                }
            }
        }
    }

    override fun onCancelled(message: String) {
        logger.warn("Sync cancelled: $message")
    }

    // ==================== Utility Methods ====================

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
