package net.easecation.bedrockloader.sync.client.listener

import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.client.model.SyncPlan
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import org.slf4j.LoggerFactory

/**
 * 控制台同步监听器
 * 将同步进度输出到日志
 * 用于阶段2（无UI）
 */
class ConsoleSyncListener : SyncListener {
    private val logger = LoggerFactory.getLogger("BedrockLoader/Sync")

    // ==================== 检查阶段 ====================

    override fun onCheckingStart() {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("开始检查资源包更新...")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    override fun onCheckingProgress(message: String) {
        logger.info("→ $message")
    }

    override fun onCheckingComplete(plan: SyncPlan) {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("检查完成！")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (plan.isUpToDate) {
            logger.info("✓ 所有资源包已是最新！")
            logger.info("  - 已同步: ${plan.upToDate.size} 个资源包")
        } else {
            logger.info("发现需要同步的资源包：")

            if (plan.toDownload.isNotEmpty()) {
                logger.info("  [新增] ${plan.toDownload.size} 个:")
                plan.toDownload.forEach { pack ->
                    logger.info("    - ${pack.name} (${formatBytes(pack.size)})")
                }
            }

            if (plan.toUpdate.isNotEmpty()) {
                logger.info("  [更新] ${plan.toUpdate.size} 个:")
                plan.toUpdate.forEach { pack ->
                    logger.info("    - ${pack.name} (${formatBytes(pack.size)})")
                }
            }

            if (plan.upToDate.isNotEmpty()) {
                logger.info("  [最新] ${plan.upToDate.size} 个")
            }

            logger.info("")
            logger.info("  总计需要下载: ${plan.totalToDownload} 个文件，${formatBytes(plan.totalBytes)}")
        }

        if (plan.localOnly.isNotEmpty()) {
            logger.info("")
            logger.info("  [仅本地] ${plan.localOnly.size} 个:")
            plan.localOnly.forEach { name ->
                logger.info("    - $name")
            }
        }

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // ==================== 下载阶段（阶段4） ====================

    override fun onDownloadStart(totalFiles: Int, totalBytes: Long) {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("开始下载资源包...")
        logger.info("总计: $totalFiles 个文件，${formatBytes(totalBytes)}")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    override fun onFileDownloadStart(file: RemotePackInfo, index: Int, total: Int) {
        logger.info("[$index/$total] 开始下载: ${file.name} (${formatBytes(file.size)})")
    }

    override fun onFileProgress(file: RemotePackInfo, bytesDownloaded: Long, totalBytes: Long) {
        val progress = (bytesDownloaded.toFloat() / totalBytes * 100).toInt()
        logger.debug("  → ${file.name}: $progress% (${formatBytes(bytesDownloaded)}/${formatBytes(totalBytes)})")
    }

    override fun onFileDownloadComplete(file: RemotePackInfo) {
        logger.info("  ✓ 完成: ${file.name}")
    }

    override fun onDownloadComplete(successCount: Int, failCount: Int) {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        if (failCount == 0) {
            logger.info("✓ 所有资源包下载完成！")
            logger.info("  成功: $successCount 个")
        } else {
            logger.warn("下载完成（部分失败）")
            logger.info("  成功: $successCount 个")
            logger.warn("  失败: $failCount 个")
        }
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // ==================== 错误和取消 ====================

    override fun onError(error: SyncError) {
        logger.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.error("✗ 同步失败")
        logger.error("错误类型: ${error.javaClass.simpleName}")
        logger.error("错误信息: ${error.message}")

        when (error) {
            is SyncError.NetworkError -> {
                logger.error("网络错误，请检查:")
                logger.error("  - 服务器地址是否正确")
                logger.error("  - 网络连接是否正常")
                logger.error("  - 服务器是否正在运行")
            }
            is SyncError.ServerError -> {
                logger.error("服务器错误 (HTTP ${error.statusCode})")
            }
            is SyncError.ConfigError -> {
                logger.error("配置文件错误，请检查配置文件")
            }
            is SyncError.FileError -> {
                logger.error("文件操作失败: ${error.filename}")
            }
            is SyncError.UnknownError -> {
                if (error.cause != null) {
                    logger.error("详细错误:", error.cause)
                }
            }
        }

        logger.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    override fun onCancelled(message: String) {
        logger.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.warn("⊗ 同步已取消")
        logger.warn("原因: $message")
        logger.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // ==================== 工具方法 ====================

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
