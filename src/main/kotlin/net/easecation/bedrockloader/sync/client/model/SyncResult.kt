package net.easecation.bedrockloader.sync.client.model

/**
 * 同步检查结果
 */
sealed class SyncResult {
    /**
     * 检查成功，返回同步计划
     */
    data class Success(val plan: SyncPlan) : SyncResult()

    /**
     * 检查失败
     */
    data class Failed(val error: SyncError) : SyncResult()

    /**
     * 配置已禁用，跳过同步
     */
    object Disabled : SyncResult() {
        override fun toString() = "Disabled"
    }

    /**
     * 服务器不可用（降级为离线模式）
     */
    data class Offline(val message: String) : SyncResult()

    /**
     * 用户取消了同步
     */
    object Cancelled : SyncResult() {
        override fun toString() = "Cancelled"
    }
}

/**
 * 下载结果（阶段4实现）
 */
sealed class DownloadResult {
    /**
     * 下载成功
     */
    data class Success(
        val downloadedFiles: List<String>,
        val failedFiles: List<String> = emptyList()
    ) : DownloadResult()

    /**
     * 下载失败
     */
    data class Failed(val error: SyncError) : DownloadResult()

    /**
     * 用户取消了下载
     */
    object Cancelled : DownloadResult() {
        override fun toString() = "Cancelled"
    }
}
