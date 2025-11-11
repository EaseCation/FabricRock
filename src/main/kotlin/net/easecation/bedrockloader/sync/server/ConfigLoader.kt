package net.easecation.bedrockloader.sync.server

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * YAML配置文件加载器
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ConfigLoader")

    /**
     * 从文件加载服务端配置
     * @param configFile 配置文件
     * @return 服务端配置对象
     */
    fun loadServerConfig(configFile: File): ServerConfig {
        if (!configFile.exists()) {
            logger.info("配置文件不存在，创建默认配置: ${configFile.absolutePath}")
            createDefaultServerConfig(configFile)
            return ServerConfig.default()
        }

        return try {
            Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                val yaml = Yaml()
                val data = yaml.load<Map<String, Any>>(reader)
                parseServerConfig(data)
            }
        } catch (e: Exception) {
            logger.error("加载配置文件失败: ${configFile.absolutePath}", e)
            logger.warn("使用默认配置")
            ServerConfig.default()
        }
    }

    /**
     * 解析YAML数据为ServerConfig对象
     */
    private fun parseServerConfig(data: Map<String, Any>): ServerConfig {
        val enabled = data["enabled"] as? Boolean ?: true
        val port = (data["port"] as? Number)?.toInt() ?: 8080
        val host = data["host"] as? String ?: "0.0.0.0"
        val baseUrl = data["baseUrl"] as? String

        return ServerConfig(
            enabled = enabled,
            port = port,
            host = host,
            baseUrl = baseUrl
        )
    }

    /**
     * 创建默认配置文件
     */
    private fun createDefaultServerConfig(configFile: File) {
        configFile.parentFile?.mkdirs()

        val defaultContent = """
            # Bedrock Loader 远程包同步服务端配置

            # 是否启用HTTP服务器
            enabled: true

            # HTTP服务器监听端口
            port: 8080

            # HTTP服务器绑定地址
            # - "0.0.0.0" 表示监听所有网络接口（局域网可访问）
            # - "127.0.0.1" 表示仅监听本地回环（仅本机可访问）
            host: "0.0.0.0"

            # 服务器基础URL（可选）
            # 如果不设置，将自动生成为 http://<host>:<port>
            # 如果服务器在内网，建议手动配置为外网可访问的地址
            # baseUrl: "http://your-server-ip:8080"
        """.trimIndent()

        Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { writer ->
            writer.write(defaultContent)
        }

        logger.info("已创建默认配置文件: ${configFile.absolutePath}")
    }
}
