package net.easecation.bedrockloader.sync.client.listener

import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.client.model.SyncPlan
import net.easecation.bedrockloader.sync.common.RemotePackInfo

/**
 * 同步监听器接口
 * 使用默认方法，实现类可以选择性覆盖需要的方法
 */
interface SyncListener {
    // ==================== 检查阶段（阶段2） ====================

    /**
     * 开始检查更新
     */
    fun onCheckingStart() {}

    /**
     * 检查进度更新
     * @param message 进度消息
     */
    fun onCheckingProgress(message: String) {}

    /**
     * 检查完成，返回同步计划
     * @param plan 同步计划
     */
    fun onCheckingComplete(plan: SyncPlan) {}

    // ==================== 下载阶段（阶段4） ====================

    /**
     * 开始下载文件
     * @param totalFiles 总文件数
     * @param totalBytes 总字节数
     */
    fun onDownloadStart(totalFiles: Int, totalBytes: Long) {}

    /**
     * 开始下载某个文件
     * @param file 文件信息
     * @param index 当前文件索引（从1开始）
     * @param total 总文件数
     */
    fun onFileDownloadStart(file: RemotePackInfo, index: Int, total: Int) {}

    /**
     * 文件下载进度更新
     * @param file 文件信息
     * @param bytesDownloaded 已下载字节数
     * @param totalBytes 总字节数
     */
    fun onFileProgress(file: RemotePackInfo, bytesDownloaded: Long, totalBytes: Long) {}

    /**
     * 文件下载完成
     * @param file 文件信息
     */
    fun onFileDownloadComplete(file: RemotePackInfo) {}

    /**
     * 所有文件下载完成
     * @param successCount 成功下载的文件数
     * @param failCount 下载失败的文件数
     */
    fun onDownloadComplete(successCount: Int, failCount: Int) {}

    // ==================== 错误和取消 ====================

    /**
     * 发生错误
     * @param error 错误信息
     */
    fun onError(error: SyncError) {}

    /**
     * 同步被取消
     * @param message 取消原因
     */
    fun onCancelled(message: String) {}
}
