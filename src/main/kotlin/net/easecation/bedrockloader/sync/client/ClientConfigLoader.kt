package net.easecation.bedrockloader.sync.client

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * 客户端配置加载器
 */
object ClientConfigLoader {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ClientConfig")

    /**
     * 加载客户端配置
     * 如果配置文件不存在，则创建默认配置文件并返回默认配置
     *
     * @param configFile 配置文件路径
     * @return 客户端配置对象
     */
    fun loadClientConfig(configFile: File): ClientConfig {
        if (!configFile.exists()) {
            logger.info("配置文件不存在，创建默认配置: ${configFile.absolutePath}")
            createDefaultClientConfig(configFile)
            return ClientConfig.default()
        }

        return try {
            logger.debug("加载客户端配置: ${configFile.absolutePath}")
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
            logger.error("加载客户端配置失败，使用默认配置", e)
            ClientConfig.default()
        }
    }

    /**
     * 保存客户端配置到文件
     *
     * @param config 配置对象
     * @param configFile 配置文件路径
     */
    fun saveConfig(config: ClientConfig, configFile: File) {
        try {
            configFile.parentFile.mkdirs()

            val configContent = """
                # Bedrock Loader - 客户端远程同步配置
                # 此配置用于客户端从服务器同步资源包

                # 是否启用远程同步
                # true: 启动时检查服务器并下载资源包
                # false: 禁用远程同步，使用本地资源包（默认）
                enabled: ${config.enabled}

                # 服务器地址（包含协议和端口）
                # 示例: http://192.168.1.100:8080
                server-url: "${config.serverUrl}"

                # HTTP请求超时时间（秒）
                # 建议值: 5-30秒
                timeout-seconds: ${config.timeoutSeconds}

                # 是否显示UI同步界面
                # true: 显示下载进度界面（阶段3）
                # false: 后台静默同步
                show-ui: ${config.showUI}

                # 发生错误时是否自动取消同步
                # true: 任何错误都取消同步，使用本地包
                # false: 尝试继续同步其他文件
                auto-cancel-on-error: ${config.autoCancelOnError}

                # 是否自动清理远程已删除的包
                # true: 自动删除remote/目录中那些远程服务器已删除的包
                # false: 保留所有本地包，不自动清理
                auto-cleanup-removed-packs: ${config.autoCleanupRemovedPacks}
            """.trimIndent()

            Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { writer ->
                writer.write(configContent)
            }

            logger.info("已保存客户端配置文件: ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("保存客户端配置文件失败", e)
            throw e
        }
    }

    /**
     * 创建默认客户端配置文件
     */
    private fun createDefaultClientConfig(configFile: File) {
        try {
            configFile.parentFile.mkdirs()

            val defaultConfig = """
                # Bedrock Loader - 客户端远程同步配置
                # 此配置用于客户端从服务器同步资源包

                # 是否启用远程同步
                # true: 启动时检查服务器并下载资源包
                # false: 禁用远程同步，使用本地资源包（默认）
                enabled: false

                # 服务器地址（包含协议和端口）
                # 示例: http://192.168.1.100:8080
                server-url: "http://localhost:8080"

                # HTTP请求超时时间（秒）
                # 建议值: 5-30秒
                timeout-seconds: 10

                # 是否显示UI同步界面
                # true: 显示下载进度界面（阶段3）
                # false: 后台静默同步
                show-ui: true

                # 发生错误时是否自动取消同步
                # true: 任何错误都取消同步，使用本地包
                # false: 尝试继续同步其他文件
                auto-cancel-on-error: false

                # 是否自动清理远程已删除的包
                # true: 自动删除remote/目录中那些远程服务器已删除的包
                # false: 保留所有本地包，不自动清理
                auto-cleanup-removed-packs: true
            """.trimIndent()

            Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { writer ->
                writer.write(defaultConfig)
            }

            logger.info("已创建默认客户端配置文件: ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("创建默认客户端配置文件失败", e)
        }
    }
}
