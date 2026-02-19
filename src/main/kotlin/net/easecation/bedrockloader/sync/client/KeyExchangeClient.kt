package net.easecation.bedrockloader.sync.client

import com.google.gson.Gson
import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.common.PackEncryption
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 客户端 Challenge-Response 密钥交换客户端
 *
 * 通过 Challenge-Response 握手从服务端获取资源包解密密钥。
 * shared_secret 由 MOD_KEY + server_token 自动派生，客户端零配置。
 */
class KeyExchangeClient(
    private val serverUrl: String,
    private val serverToken: String,
    private val timeout: Int = 10
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/KeyExchange")
    private val gson = Gson()

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeout.toLong()))
        .build()

    // 从 server_token 派生 shared_secret
    private val sharedSecret: String = PackEncryption.deriveSharedSecret(serverToken)

    /**
     * 通过 Challenge-Response 获取指定包的解密密钥
     *
     * 1. POST /keys/challenge -> 获取 challenge
     * 2. 用 sharedSecret 计算 HMAC
     * 3. POST /keys/exchange -> 提交 HMAC 换取密钥
     *
     * @param filename 包文件名
     * @return AES-256 解密密钥（64字符十六进制）
     */
    fun fetchKey(filename: String): String {
        logger.debug("Fetching key for: $filename")

        // Step 1: 获取 challenge
        val challenge = requestChallenge()

        // Step 2: 计算 HMAC
        val hmac = PackEncryption.computeHmac(sharedSecret, challenge, filename)

        // Step 3: 交换密钥
        return exchangeKey(challenge, filename, hmac)
    }

    /**
     * 批量获取多个包的密钥（串行，每个包一次握手）
     *
     * @param filenames 包文件名列表
     * @return 文件名 -> 密钥的映射
     */
    fun fetchKeys(filenames: List<String>): Map<String, String> {
        val keys = mutableMapOf<String, String>()
        for (filename in filenames) {
            try {
                keys[filename] = fetchKey(filename)
                logger.info("Key obtained for: $filename")
            } catch (e: Exception) {
                logger.error("Failed to fetch key for: $filename", e)
                throw e
            }
        }
        return keys
    }

    /**
     * POST /keys/challenge -> 获取 challenge
     */
    private fun requestChallenge(): String {
        val url = "${serverUrl.trimEnd('/')}/keys/challenge"

        val body = gson.toJson(mapOf("client_id" to PackEncryption.generateChallenge()))

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .header("Content-Type", "application/json")
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw SyncError.ServerError(response.statusCode(), "Challenge request failed: HTTP ${response.statusCode()}")
            }

            @Suppress("UNCHECKED_CAST")
            val result = gson.fromJson(response.body(), Map::class.java) as Map<String, Any>
            val challenge = result["challenge"] as? String
                ?: throw SyncError.ServerError(200, "Invalid challenge response: missing 'challenge' field")

            logger.debug("Challenge obtained: ${challenge.take(8)}...")
            return challenge
        } catch (e: SyncError) {
            throw e
        } catch (e: Exception) {
            throw SyncError.NetworkError("Challenge request failed: ${e.message}", e)
        }
    }

    /**
     * POST /keys/exchange -> 提交 HMAC 换取密钥
     */
    private fun exchangeKey(challenge: String, filename: String, hmac: String): String {
        val url = "${serverUrl.trimEnd('/')}/keys/exchange"

        val body = gson.toJson(mapOf(
            "challenge" to challenge,
            "filename" to filename,
            "hmac" to hmac
        ))

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(timeout.toLong()))
            .header("Content-Type", "application/json")
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 403) {
                throw SyncError.ServerError(403, "Key exchange authentication failed for: $filename")
            }

            if (response.statusCode() != 200) {
                throw SyncError.ServerError(response.statusCode(), "Key exchange failed: HTTP ${response.statusCode()}")
            }

            @Suppress("UNCHECKED_CAST")
            val result = gson.fromJson(response.body(), Map::class.java) as Map<String, Any>
            val key = result["key"] as? String
                ?: throw SyncError.ServerError(200, "Invalid key exchange response: missing 'key' field")

            logger.debug("Key exchanged for: $filename")
            return key
        } catch (e: SyncError) {
            throw e
        } catch (e: Exception) {
            throw SyncError.NetworkError("Key exchange failed: ${e.message}", e)
        }
    }
}
