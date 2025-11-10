package net.easecation.bedrockloader.sync.client.model

import net.easecation.bedrockloader.sync.common.RemotePackInfo

/**
 * 同步计划
 * 包含需要下载、更新、已最新和仅本地的包列表
 */
data class SyncPlan(
    /**
     * 需要下载的新包
     */
    val toDownload: List<RemotePackInfo> = emptyList(),

    /**
     * 需要更新的包（MD5不匹配）
     */
    val toUpdate: List<RemotePackInfo> = emptyList(),

    /**
     * 已经是最新的包（MD5匹配）
     */
    val upToDate: List<String> = emptyList(),

    /**
     * 仅本地存在的包（服务器上没有）
     */
    val localOnly: List<String> = emptyList(),

    /**
     * 需要清理的包（远程已删除，且在remote/目录中）
     * 注意：这些包是远程下载的包，现在远程服务器已删除
     * 只有启用autoCleanupRemovedPacks时才会填充此列表
     */
    val packagesToCleanup: List<String> = emptyList(),

    /**
     * UUID冲突列表
     * 远程包与本地手动包的UUID相同，优先使用本地包
     */
    val uuidConflicts: List<UUIDConflict> = emptyList()
) {
    /**
     * UUID冲突信息
     * 记录远程包和本地包的UUID冲突
     */
    data class UUIDConflict(
        val remotePackName: String,    // 远程包文件名
        val remoteUUID: String,         // 远程包UUID
        val localPackName: String,      // 本地包文件名
        val localIsManual: Boolean      // 本地包是否为手动放置
    )
    /**
     * 需要下载的包总数（新增 + 更新）
     */
    val totalToDownload: Int
        get() = toDownload.size + toUpdate.size

    /**
     * 需要下载的总字节数
     */
    val totalBytes: Long
        get() = (toDownload + toUpdate).sumOf { it.size }

    /**
     * 是否有需要下载的内容
     */
    val hasDownloads: Boolean
        get() = totalToDownload > 0

    /**
     * 是否已经是最新状态（没有需要下载的）
     */
    val isUpToDate: Boolean
        get() = totalToDownload == 0

    override fun toString(): String {
        return "SyncPlan(新增=${toDownload.size}, 更新=${toUpdate.size}, 最新=${upToDate.size}, 仅本地=${localOnly.size}, 待清理=${packagesToCleanup.size}, UUID冲突=${uuidConflicts.size})"
    }
}
