package net.easecation.bedrockloader.sync.client

/**
 * 客户端同步配置
 */
data class ClientConfig(
    /**
     * 是否启用远程同步
     */
    val enabled: Boolean = false,

    /**
     * 服务器地址（包含协议和端口）
     * 例如: http://localhost:8080
     */
    val serverUrl: String = "http://localhost:8080",

    /**
     * HTTP请求超时时间（秒）
     */
    val timeoutSeconds: Int = 10,

    /**
     * 是否显示UI界面（阶段3使用）
     */
    val showUI: Boolean = true,

    /**
     * 发生错误时是否自动取消同步
     * true: 发生任何错误都取消，使用本地包
     * false: 尝试继续同步（部分文件失败不影响其他文件）
     */
    val autoCancelOnError: Boolean = false,

    /**
     * 是否自动清理远程已删除的包
     * true: 自动删除remote/目录中那些远程服务器已删除的包
     * false: 保留所有本地包，不自动清理
     * 注意：仅清理remote/目录中的包，手动放置的包永远不会被清理
     */
    val autoCleanupRemovedPacks: Boolean = true
) {
    companion object {
        /**
         * 默认配置
         */
        fun default(): ClientConfig = ClientConfig()
    }
}
