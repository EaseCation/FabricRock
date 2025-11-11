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
 * HTTP Client
 * Fetches resource pack manifest from server
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
     * Fetch manifest.json from server
     *
     * @return Remote pack manifest
     * @throws SyncError.NetworkError Network connection error
     * @throws SyncError.ServerError Server returned error status code
     */
    fun fetchManifest(): RemotePackManifest {
        val url = "${config.serverUrl.trimEnd('/')}/manifest.json"

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
                    return parseManifest(response.body())
                }
                else -> {
                    val errorMessage = "Server error: HTTP ${response.statusCode()}"
                    logger.error(errorMessage)
                    throw SyncError.ServerError(response.statusCode(), errorMessage)
                }
            }
        } catch (e: ConnectException) {
            val errorMessage = "Cannot connect to server: ${config.serverUrl}"
            logger.warn(errorMessage)
            throw SyncError.NetworkError(errorMessage, e)
        } catch (e: java.net.SocketTimeoutException) {
            val errorMessage = "Connection timeout (${config.timeoutSeconds}s)"
            logger.warn(errorMessage)
            throw SyncError.NetworkError(errorMessage, e)
        } catch (e: SyncError) {
            // Already SyncError, throw directly
            throw e
        } catch (e: Exception) {
            val errorMessage = "Failed to fetch manifest: ${e.message}"
            logger.error(errorMessage, e)
            throw SyncError.NetworkError(errorMessage, e)
        }
    }

    /**
     * Parse JSON string to manifest object
     */
    private fun parseManifest(json: String): RemotePackManifest {
        return try {
            gson.fromJson(json, RemotePackManifest::class.java)
        } catch (e: Exception) {
            val errorMessage = "Failed to parse manifest: ${e.message}"
            logger.error(errorMessage, e)
            throw SyncError.ServerError(200, errorMessage)
        }
    }

    /**
     * Test server connection
     *
     * @return true: Server accessible, false: Server not accessible
     */
    fun testConnection(): Boolean {
        val url = "${config.serverUrl.trimEnd('/')}/ping"

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val connected = response.statusCode() == 200

            if (connected) {
                logger.info("Server connection successful")
            } else {
                logger.warn("Server returned non-200 status: ${response.statusCode()}")
            }

            connected
        } catch (e: Exception) {
            logger.warn("Server connection failed: ${e.message}")
            false
        }
    }
}
