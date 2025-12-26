package net.easecation.bedrockloader.sync.server

import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.json.JsonMapper
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Type

/**
 * Embedded HTTP Server
 *
 * Lightweight HTTP server based on Javalin for resource pack distribution
 */
class EmbeddedHttpServer(
    private val config: ServerConfig,
    private val packDirectory: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/HttpServer")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var app: Javalin? = null
    private val manifestGenerator = ManifestGenerator(packDirectory, config)

    /**
     * Start HTTP server
     */
    fun start() {
        if (app != null) {
            logger.warn("HTTP server is already running")
            return
        }

        try {
            logger.info("Starting HTTP server on ${config.host}:${config.port}...")

            app = Javalin.create { javalinConfig ->
                // 禁用Javalin横幅
                javalinConfig.showJavalinBanner = false

                // 配置Gson JSON映射器
                javalinConfig.jsonMapper(object : JsonMapper {
                    override fun toJsonString(obj: Any, type: Type): String {
                        return gson.toJson(obj, type)
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun <T : Any> fromJsonString(json: String, targetType: Type): T {
                        return gson.fromJson(json, targetType) as T
                    }
                })

                // 配置日志
                javalinConfig.requestLogger.http { ctx, ms ->
                    logger.info("${ctx.method()} ${ctx.path()} - ${ctx.status()} (${ms}ms)")
                }
            }.start(config.host, config.port)

            // Register routes
            registerRoutes()

            logger.info("HTTP server started successfully at ${config.getEffectiveBaseUrl()}")

        } catch (e: Exception) {
            logger.error("Failed to start HTTP server", e)
            throw e
        }
    }

    /**
     * Stop HTTP server
     */
    fun stop() {
        if (app == null) {
            logger.warn("HTTP server is not running")
            return
        }

        try {
            logger.info("Stopping HTTP server...")
            app?.stop()
            app = null
            logger.info("HTTP server stopped")
        } catch (e: Exception) {
            logger.error("Failed to stop HTTP server", e)
        }
    }

    /**
     * Register all REST API routes
     */
    private fun registerRoutes() {
        val javalin = app ?: return

        // GET /ping - Health check
        javalin.get("/ping") { ctx ->
            handlePing(ctx)
        }

        // GET /manifest.json - Get resource pack manifest
        javalin.get("/manifest.json") { ctx ->
            handleGetManifest(ctx)
        }

        // GET /packs/{filename} - Download resource pack
        javalin.get("/packs/{filename}") { ctx ->
            handleDownloadPack(ctx)
        }

        // 404 handler
        javalin.error(404) { ctx ->
            ctx.json(mapOf(
                "error" to "Not Found",
                "message" to "Resource not found: ${ctx.path()}"
            ))
        }

        // 500 handler
        javalin.error(500) { ctx ->
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "Internal server error"
            ))
        }
    }

    /**
     * 处理健康检查请求
     * GET /ping
     */
    private fun handlePing(ctx: Context) {
        ctx.json(mapOf(
            "status" to "ok",
            "version" to "1.0.0",
            "timestamp" to System.currentTimeMillis(),
            "message" to "Bedrock Loader Remote Pack Sync Server"
        ))
    }

    /**
     * Handle manifest request
     * GET /manifest.json
     */
    private fun handleGetManifest(ctx: Context) {
        try {
            val manifest = manifestGenerator.generate()

            // Return JSON response
            ctx.contentType("application/json")
            ctx.result(gson.toJson(manifest))

        } catch (e: Exception) {
            logger.error("Failed to generate manifest", e)
            ctx.status(500)
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "Failed to generate manifest: ${e.message}"
            ))
        }
    }

    /**
     * Handle resource pack download request
     * GET /packs/{filename}
     */
    private fun handleDownloadPack(ctx: Context) {
        val filename = ctx.pathParam("filename")

        // Security check: prevent path traversal attacks
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("Invalid filename detected: $filename")
            ctx.status(400)
            ctx.json(mapOf(
                "error" to "Bad Request",
                "message" to "Invalid filename"
            ))
            return
        }

        // Look for packed files in cache directory first
        val cacheDir = File(packDirectory, ".cache")
        var file = File(cacheDir, filename)

        // Fallback to main directory for original ZIP/MCPACK files
        if (!file.exists() || !file.isFile) {
            file = File(packDirectory, filename)
        }

        // Check if file exists
        if (!file.exists() || !file.isFile) {
            logger.warn("File not found: $filename")
            ctx.status(404)
            ctx.json(mapOf(
                "error" to "Not Found",
                "message" to "Resource pack not found: $filename"
            ))
            return
        }

        // Check file type (only allow .zip, .mcpack, and .mcaddon)
        if (!filename.endsWith(".zip") && !filename.endsWith(".mcpack") && !filename.endsWith(".mcaddon")) {
            logger.warn("Forbidden file type: $filename")
            ctx.status(403)
            ctx.json(mapOf(
                "error" to "Forbidden",
                "message" to "File type not allowed"
            ))
            return
        }

        try {
            // Send file
            ctx.contentType("application/octet-stream")
            ctx.header("Content-Disposition", "attachment; filename=\"$filename\"")

            // For resource pack files (typically a few MB), reading into memory is safe
            // This avoids stream lifecycle management issues
            ctx.result(file.readBytes())

        } catch (e: Exception) {
            logger.error("Failed to transfer file: $filename", e)
            ctx.status(500)
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "File transfer failed: ${e.message}"
            ))
        }
    }

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean {
        return app != null
    }
}
