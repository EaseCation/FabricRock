package net.easecation.bedrockloader.sync.client.ui

import net.easecation.bedrockloader.sync.client.listener.SyncListener
import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.client.model.SyncPlan
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities

/**
 * Swing UI监听器
 * 实现SyncListener接口，将同步事件转换为UI更新
 */
class SwingSyncListener(
    private val window: DownloadWindow
) : SyncListener {

    private val logger = LoggerFactory.getLogger("BedrockLoader/SwingSyncListener")

    // 下载统计
    private var downloadStartTime: Long = 0
    private var lastUpdateTime: Long = 0
    private var lastDownloadedBytes: Long = 0
    private var totalDownloadedBytes: Long = 0
    private var totalBytesToDownload: Long = 0

    // UI更新频率控制（毫秒）
    private val UI_UPDATE_INTERVAL_MS = 100

    // ==================== 检查阶段 ====================

    override fun onCheckingStart() {
        logger.debug("检查开始 - 更新UI")
        SwingUtilities.invokeLater {
            window.updateStatus("正在连接服务器...")
            window.updateProgress(0)
            window.clearDetails()
        }
    }

    override fun onCheckingProgress(message: String) {
        logger.debug("检查进度: $message")
        SwingUtilities.invokeLater {
            window.updateStatus(message)
        }
    }

    override fun onCheckingComplete(plan: SyncPlan) {
        logger.debug("检查完成 - 发现 ${plan.totalToDownload} 个需要下载的文件")
        SwingUtilities.invokeLater {
            if (plan.isUpToDate) {
                window.updateStatus("所有资源包已是最新")
                window.updateProgress(100)
                window.bringToFront()

                // 延迟1.5秒后自动关闭窗口
                Thread {
                    Thread.sleep(1500)
                    window.close()
                }.start()
            } else {
                window.updateStatus("发现 ${plan.totalToDownload} 个需要下载的文件")
                val totalSize = UIUtil.formatBytes(plan.totalBytes)
                window.updateCurrentFile("准备下载，总大小: $totalSize")
            }
        }
    }

    // ==================== 下载阶段 ====================

    override fun onDownloadStart(totalFiles: Int, totalBytes: Long) {
        logger.debug("下载开始 - 总文件数: $totalFiles, 总大小: ${UIUtil.formatBytes(totalBytes)}")
        downloadStartTime = System.currentTimeMillis()
        lastUpdateTime = downloadStartTime
        lastDownloadedBytes = 0
        totalDownloadedBytes = 0
        totalBytesToDownload = totalBytes

        SwingUtilities.invokeLater {
            window.updateStatus("开始下载 $totalFiles 个文件...")
            window.updateProgress(0)
            window.clearDetails()
        }
    }

    override fun onFileDownloadStart(file: RemotePackInfo, index: Int, total: Int) {
        logger.debug("文件下载开始: ${file.name} ($index/$total)")
        SwingUtilities.invokeLater {
            window.updateStatus("下载中... ($index/$total)")
            window.updateCurrentFile(file.name)
        }
    }

    override fun onFileProgress(file: RemotePackInfo, bytesDownloaded: Long, totalBytes: Long) {
        val currentTime = System.currentTimeMillis()

        // 限制UI更新频率
        if (currentTime - lastUpdateTime < UI_UPDATE_INTERVAL_MS && bytesDownloaded < totalBytes) {
            return
        }

        // 计算下载速度（基于总下载量，而不是单个文件）
        val timeDiff = currentTime - lastUpdateTime
        val bytesDiff = bytesDownloaded - lastDownloadedBytes
        val speed = if (timeDiff > 0) (bytesDiff * 1000 / timeDiff) else 0L

        // 更新统计
        lastUpdateTime = currentTime
        lastDownloadedBytes = bytesDownloaded

        // 计算总体进度
        val filePercent = UIUtil.calculatePercent(bytesDownloaded, totalBytes)
        val totalPercent = if (totalBytesToDownload > 0) {
            UIUtil.calculatePercent(totalDownloadedBytes + bytesDownloaded, totalBytesToDownload)
        } else {
            filePercent
        }

        logger.trace("文件进度: ${file.name} - $filePercent%, 总进度: $totalPercent%")

        SwingUtilities.invokeLater {
            window.updateProgress(totalPercent)
            window.updateDetails(
                currentFile = file.name,
                downloaded = totalDownloadedBytes + bytesDownloaded,
                total = totalBytesToDownload,
                speed = speed
            )
        }
    }

    override fun onFileDownloadComplete(file: RemotePackInfo) {
        logger.debug("文件下载完成: ${file.name}")
        totalDownloadedBytes += file.size
        lastDownloadedBytes = 0  // 重置为下一个文件准备
    }

    override fun onDownloadComplete(successCount: Int, failCount: Int) {
        logger.info("下载完成 - 成功: $successCount, 失败: $failCount")
        SwingUtilities.invokeLater {
            if (failCount == 0) {
                window.updateStatus("下载完成！")
                window.updateProgress(100)
                window.showSuccess()
                window.bringToFront()

                // 延迟3秒后关闭窗口
                Thread {
                    Thread.sleep(3000)
                    window.close()
                }.start()
            } else {
                window.updateStatus("下载完成（部分失败）")
                window.updateCurrentFile("成功: $successCount, 失败: $failCount")
                // 切换到关闭模式，让用户手动关闭窗口
                window.switchToCloseMode()
            }
        }
    }

    // ==================== 错误和取消 ====================

    override fun onError(error: SyncError) {
        logger.error("同步错误: ${error.message}")
        SwingUtilities.invokeLater {
            window.showError(
                error = error,
                onContinue = {
                    logger.info("用户选择继续启动游戏")
                    window.close()
                },
                onExit = {
                    logger.info("用户选择退出游戏")
                    System.exit(1)
                }
            )
        }
    }

    override fun onCancelled(message: String) {
        logger.warn("同步已取消: $message")
        SwingUtilities.invokeLater {
            window.updateStatus("已取消")
            window.disableCancelButton()

            // 延迟关闭窗口
            Thread {
                Thread.sleep(1000)
                window.close()
            }.start()
        }
    }
}
