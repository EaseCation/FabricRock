package net.easecation.bedrockloader.sync.client.model

/**
 * 同步错误类型
 * 继承Exception以支持throw和catch
 */
sealed class SyncError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * 配置文件错误
     */
    class ConfigError(
        message: String,
        cause: Throwable? = null
    ) : SyncError(message, cause)

    /**
     * 网络连接错误
     */
    class NetworkError(
        message: String,
        cause: Throwable? = null
    ) : SyncError(message, cause)

    /**
     * 服务器HTTP错误
     */
    class ServerError(
        val statusCode: Int,
        message: String
    ) : SyncError(message)

    /**
     * 文件操作错误
     */
    class FileError(
        val filename: String,
        message: String,
        cause: Throwable? = null
    ) : SyncError(message, cause)

    /**
     * 未知错误
     */
    class UnknownError(
        message: String,
        cause: Throwable? = null
    ) : SyncError(message, cause)
}
