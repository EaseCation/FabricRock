package net.easecation.bedrockloader.sync.server

/**
 * 远程包同步服务端配置
 */
data class ServerConfig(
    /**
     * 是否启用HTTP服务器
     */
    val enabled: Boolean = true,

    /**
     * HTTP服务器监听端口
     */
    val port: Int = 8080,

    /**
     * HTTP服务器绑定地址
     * - "0.0.0.0" 表示监听所有网络接口
     * - "127.0.0.1" 表示仅监听本地回环
     */
    val host: String = "0.0.0.0",

    /**
     * 服务器基础URL（用于生成下载链接）
     * 如果为null，将自动生成为 http://<host>:<port>
     */
    val baseUrl: String? = null,

    /**
     * 是否启用资源包加密
     * 启用后，客户端下载的资源包将被加密，明文不会写入磁盘
     */
    val encryptionEnabled: Boolean = false,

    /**
     * 加密用的服务端密钥
     * - "auto": 启动时自动生成并持久化到 .keys.json
     * - 其他值: 使用指定的密钥字符串
     */
    val encryptionServerSecret: String = "auto"
) {
    /**
     * 获取有效的基础URL
     */
    fun getEffectiveBaseUrl(): String {
        return baseUrl ?: "http://$host:$port"
    }

    companion object {
        /**
         * 默认配置
         */
        fun default(): ServerConfig = ServerConfig()
    }
}
