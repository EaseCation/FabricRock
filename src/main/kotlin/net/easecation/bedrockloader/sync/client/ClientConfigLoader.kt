package net.easecation.bedrockloader.sync.client

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Client configuration loader
 */
object ClientConfigLoader {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ClientConfig")

    /**
     * Load client configuration
     * If the config file does not exist, create default config file and return default config
     *
     * @param configFile Path to the config file
     * @return Client configuration object
     */
    fun loadClientConfig(configFile: File): ClientConfig {
        if (!configFile.exists()) {
            logger.info("Config file does not exist, creating default config: ${configFile.absolutePath}")
            createDefaultClientConfig(configFile)
            return ClientConfig.default()
        }

        return try {
            logger.debug("Loading client config: ${configFile.absolutePath}")
            Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                val yaml = Yaml()
                val data = yaml.load<Map<String, Any>>(reader) ?: emptyMap()

                ClientConfig(
                    enabled = data["enabled"] as? Boolean ?: false,
                    serverUrl = data["server-url"] as? String ?: "http://localhost:8080",
                    timeoutSeconds = data["timeout-seconds"] as? Int ?: 10,
                    showUI = data["show-ui"] as? Boolean ?: true,
                    autoCancelOnError = data["auto-cancel-on-error"] as? Boolean ?: false,
                    autoCleanupRemovedPacks = data["auto-cleanup-removed-packs"] as? Boolean ?: true
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to load client config, using default config", e)
            ClientConfig.default()
        }
    }

    /**
     * Save client configuration to file
     *
     * @param config Configuration object
     * @param configFile Path to the config file
     */
    fun saveConfig(config: ClientConfig, configFile: File) {
        try {
            configFile.parentFile.mkdirs()

            val configContent = """
                # Bedrock Loader - Client Remote Sync Configuration
                # This configuration is used for syncing resource packs from server

                # Enable remote synchronization
                # true: Check server and download resource packs on startup
                # false: Disable remote sync, use local resource packs (default)
                enabled: ${config.enabled}

                # Server address (including protocol and port)
                # Example: http://192.168.1.100:8080
                server-url: "${config.serverUrl}"

                # HTTP request timeout (seconds)
                # Recommended: 5-30 seconds
                timeout-seconds: ${config.timeoutSeconds}

                # Show UI sync interface
                # true: Show download progress UI (Phase 3)
                # false: Silent background sync
                show-ui: ${config.showUI}

                # Auto cancel on error
                # true: Cancel sync on any error, use local packs
                # false: Try to continue syncing other files
                auto-cancel-on-error: ${config.autoCancelOnError}

                # Auto cleanup removed packs
                # true: Automatically delete packs from remote/ directory that have been removed from server
                # false: Keep all local packs, no auto cleanup
                auto-cleanup-removed-packs: ${config.autoCleanupRemovedPacks}
            """.trimIndent()

            Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { writer ->
                writer.write(configContent)
            }

            logger.info("Saved client config file: ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save client config file", e)
            throw e
        }
    }

    /**
     * Create default client configuration file
     */
    private fun createDefaultClientConfig(configFile: File) {
        try {
            configFile.parentFile.mkdirs()

            val defaultConfig = """
                # Bedrock Loader - Client Remote Sync Configuration
                # This configuration is used for syncing resource packs from server

                # Enable remote synchronization
                # true: Check server and download resource packs on startup
                # false: Disable remote sync, use local resource packs (default)
                enabled: false

                # Server address (including protocol and port)
                # Example: http://192.168.1.100:8080
                server-url: "http://localhost:8080"

                # HTTP request timeout (seconds)
                # Recommended: 5-30 seconds
                timeout-seconds: 10

                # Show UI sync interface
                # true: Show download progress UI (Phase 3)
                # false: Silent background sync
                show-ui: true

                # Auto cancel on error
                # true: Cancel sync on any error, use local packs
                # false: Try to continue syncing other files
                auto-cancel-on-error: false

                # Auto cleanup removed packs
                # true: Automatically delete packs from remote/ directory that have been removed from server
                # false: Keep all local packs, no auto cleanup
                auto-cleanup-removed-packs: true
            """.trimIndent()

            Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { writer ->
                writer.write(defaultConfig)
            }

            logger.info("Created default client config file: ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create default client config file", e)
        }
    }
}
