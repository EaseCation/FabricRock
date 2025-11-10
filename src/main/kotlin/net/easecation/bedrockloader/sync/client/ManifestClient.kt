package net.easecation.bedrockloader.sync.client

import com.google.gson.Gson
import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * HTTP客户端，用于从服务器获取资源包manifest
 */
class ManifestClient(
    private val config: ClientConfig
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ManifestClient")
    private val gson = Gson()

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
        .build()

    /**
     * 从服务器获取manifest.json
     *
     * @return 远程资源包manifest
     * @throws SyncError.NetworkError 网络连接错误
     * @throws SyncError.ServerError 服务器返回错误状态码
     */
    fun fetchManifest(): RemotePackManifest {
        val url = "${config.serverUrl.trimEnd('/')}/manifest.json"
        logger.debug("正在获取manifest: $url")

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                .header("Accept", "application/json")
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            when (response.statusCode()) {
                200 -> {
                    logger.debug("成功获取manifest，长度: ${response.body().length}")
                    return parseManifest(response.body())
                }
                else -> {
                    val errorMessage = "服务器返回错误: HTTP ${response.statusCode()}"
                    logger.error(errorMessage)
                    throw SyncError.ServerError(response.statusCode(), errorMessage)
                }
            }
        } catch (e: ConnectException) {
            val errorMessage = "无法连接到服务器: ${config.serverUrl}"
            logger.warn(errorMessage)
            throw SyncError.NetworkError(errorMessage, e)
        } catch (e: java.net.SocketTimeoutException) {
            val errorMessage = "连接超时 (${config.timeoutSeconds}秒)"
            logger.warn(errorMessage)
            throw SyncError.NetworkError(errorMessage, e)
        } catch (e: SyncError) {
            // 已经是SyncError，直接抛出
            throw e
        } catch (e: Exception) {
            val errorMessage = "获取manifest失败: ${e.message}"
            logger.error(errorMessage, e)
            throw SyncError.NetworkError(errorMessage, e)
        }
    }

    /**
     * 解析JSON字符串为manifest对象
     */
    private fun parseManifest(json: String): RemotePackManifest {
        return try {
            gson.fromJson(json, RemotePackManifest::class.java)
        } catch (e: Exception) {
            val errorMessage = "解析manifest失败: ${e.message}"
            logger.error(errorMessage, e)
            throw SyncError.ServerError(200, errorMessage)
        }
    }

    /**
     * 测试服务器连接
     *
     * @return true: 服务器可访问，false: 服务器不可访问
     */
    fun testConnection(): Boolean {
        val url = "${config.serverUrl.trimEnd('/')}/ping"
        logger.debug("测试服务器连接: $url")

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val connected = response.statusCode() == 200

            if (connected) {
                logger.info("服务器连接成功")
            } else {
                logger.warn("服务器返回非200状态: ${response.statusCode()}")
            }

            connected
        } catch (e: Exception) {
            logger.warn("服务器连接失败: ${e.message}")
            false
        }
    }
}
