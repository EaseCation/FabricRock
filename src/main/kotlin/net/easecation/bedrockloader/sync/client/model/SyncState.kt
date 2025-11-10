package net.easecation.bedrockloader.sync.client.model

/**
 * 同步状态机
 */
sealed class SyncState {
    /**
     * 空闲状态（未开始）
     */
    object Idle : SyncState() {
        override fun toString() = "Idle"
    }

    /**
     * 正在加载配置
     */
    object LoadingConfig : SyncState() {
        override fun toString() = "LoadingConfig"
    }

    /**
     * 正在获取远程manifest
     */
    object FetchingManifest : SyncState() {
        override fun toString() = "FetchingManifest"
    }

    /**
     * 正在对比本地和远程包
     */
    object ComparingPacks : SyncState() {
        override fun toString() = "ComparingPacks"
    }

    /**
     * 同步计划已生成，等待执行下载
     */
    data class PlanReady(val plan: SyncPlan) : SyncState()

    /**
     * 正在下载文件（阶段4）
     */
    data class Downloading(
        val current: Int,
        val total: Int,
        val progress: Float
    ) : SyncState()

    /**
     * 同步完成
     */
    object Complete : SyncState() {
        override fun toString() = "Complete"
    }

    /**
     * 同步出错
     */
    data class Error(val error: SyncError) : SyncState()

    /**
     * 用户取消了同步
     */
    object Cancelled : SyncState() {
        override fun toString() = "Cancelled"
    }
}
