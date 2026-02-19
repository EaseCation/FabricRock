package net.easecation.bedrockloader.standalone

import net.easecation.bedrockloader.sync.server.*
import net.easecation.bedrockloader.sync.common.PackEncryption
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("BedrockPackServer")

    val options = parseArgs(args)

    logger.info("Bedrock Pack Distribution Server v1.0.0")
    logger.info("========================================")

    // 确定包目录
    val packDir = File(options.packDir)
    if (!packDir.exists()) {
        packDir.mkdirs()
        logger.info("Created pack directory: ${packDir.absolutePath}")
    }

    // 确保缓存目录存在
    val cacheDir = File(packDir, ".cache")
    if (!cacheDir.exists()) cacheDir.mkdirs()

    // 加载配置
    val configFile = File(options.configFile ?: "${packDir.absolutePath}/server.yml")
    val config = ConfigLoader.loadServerConfig(configFile)

    // 命令行参数覆盖配置文件
    val effectiveConfig = config.copy(
        port = options.port ?: config.port,
        host = options.host ?: config.host,
        baseUrl = options.baseUrl ?: config.baseUrl,
        enabled = true
    )

    logger.info("Pack directory: ${packDir.absolutePath}")
    logger.info("Config file: ${configFile.absolutePath}")
    logger.info("Server: ${effectiveConfig.host}:${effectiveConfig.port}")

    // 扫描包目录并填充 BedrockPackRegistry
    logger.info("Scanning packs...")
    StandalonePackScanner.scanAndRegister(packDir, cacheDir)

    // 初始化加密组件
    var keyManager: PackKeyManager? = null
    var encryptedPackCache: EncryptedPackCache? = null
    var challengeManager: ChallengeManager? = null
    var serverToken: String? = null

    if (effectiveConfig.encryptionEnabled) {
        logger.info("Initializing encryption system...")

        val serverSecret = resolveServerSecret(effectiveConfig, packDir)
        serverToken = PackEncryption.generateServerToken(serverSecret)
        val sharedSecret = PackEncryption.deriveSharedSecretFromServerSecret(serverSecret)

        val keyFile = File(packDir, ".keys.json")
        keyManager = PackKeyManager(keyFile)
        encryptedPackCache = EncryptedPackCache(packDir, keyManager)
        challengeManager = ChallengeManager(sharedSecret)

        logger.info("Encryption enabled (server_token: ${serverToken.take(16)}...)")
    }

    // 启动 HTTP 服务器
    val server = EmbeddedHttpServer(
        effectiveConfig, packDir, keyManager, encryptedPackCache, challengeManager, serverToken
    )
    server.start()

    // 优雅关闭
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        server.stop()
    })

    logger.info("Server running at ${effectiveConfig.getEffectiveBaseUrl()}")
    logger.info("Press Ctrl+C to stop")

    // 阻塞主线程
    Thread.currentThread().join()
}

private fun resolveServerSecret(config: ServerConfig, packDir: File): String {
    return if (config.encryptionServerSecret == "auto") {
        val secretFile = File(packDir, ".server_secret")
        if (secretFile.exists()) {
            secretFile.readText().trim()
        } else {
            val generated = PackEncryption.generateKey()
            secretFile.writeText(generated)
            LoggerFactory.getLogger("BedrockPackServer")
                .info("Auto-generated server_secret saved to: ${secretFile.absolutePath}")
            generated
        }
    } else {
        config.encryptionServerSecret
    }
}

data class ServerOptions(
    val packDir: String = "config/bedrock-loader",
    val configFile: String? = null,
    val port: Int? = null,
    val host: String? = null,
    val baseUrl: String? = null
)

fun parseArgs(args: Array<String>): ServerOptions {
    var packDir = "config/bedrock-loader"
    var configFile: String? = null
    var port: Int? = null
    var host: String? = null
    var baseUrl: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--pack-dir", "-d" -> {
                i++
                if (i >= args.size) error("Missing value for ${args[i - 1]}")
                packDir = args[i]
            }
            "--config", "-c" -> {
                i++
                if (i >= args.size) error("Missing value for ${args[i - 1]}")
                configFile = args[i]
            }
            "--port", "-p" -> {
                i++
                if (i >= args.size) error("Missing value for ${args[i - 1]}")
                port = args[i].toIntOrNull() ?: error("Invalid port: ${args[i]}")
            }
            "--host" -> {
                i++
                if (i >= args.size) error("Missing value for ${args[i - 1]}")
                host = args[i]
            }
            "--base-url" -> {
                i++
                if (i >= args.size) error("Missing value for ${args[i - 1]}")
                baseUrl = args[i]
            }
            "--help" -> {
                printHelp()
                System.exit(0)
            }
            else -> {
                System.err.println("Unknown option: ${args[i]}")
                printHelp()
                System.exit(1)
            }
        }
        i++
    }

    return ServerOptions(packDir, configFile, port, host, baseUrl)
}

fun printHelp() {
    println("""
        Bedrock Pack Distribution Server

        Usage: java -jar bedrock-pack-server.jar [options]

        Options:
          -d, --pack-dir <path>   Pack directory (default: config/bedrock-loader)
          -c, --config <path>     Config file path (default: <pack-dir>/server.yml)
          -p, --port <port>       Server port (overrides config)
          --host <host>           Server host (overrides config)
          --base-url <url>        Base URL for download links (overrides config)
          --help                  Show this help message
    """.trimIndent())
}
