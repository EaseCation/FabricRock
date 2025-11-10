package net.easecation.bedrockloader.sync.server

import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.json.JsonMapper
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Type

/**
 * 嵌入式HTTP服务器
 *
 * 基于Javalin实现的轻量级HTTP服务器，用于提供资源包下载服务
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
     * 启动HTTP服务器
     */
    fun start() {
        if (app != null) {
            logger.warn("HTTP服务器已经在运行")
            return
        }

        try {
            logger.info("正在启动HTTP服务器...")
            logger.info("配置: 端口=${config.port}, 绑定地址=${config.host}")

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

            // 注册路由
            registerRoutes()

            logger.info("HTTP服务器启动成功！")
            logger.info("访问地址: ${config.getEffectiveBaseUrl()}")
            logger.info("  - 健康检查: ${config.getEffectiveBaseUrl()}/ping")
            logger.info("  - 资源包清单: ${config.getEffectiveBaseUrl()}/manifest.json")
            logger.info("  - 资源包下载: ${config.getEffectiveBaseUrl()}/packs/<filename>")

        } catch (e: Exception) {
            logger.error("HTTP服务器启动失败", e)
            throw e
        }
    }

    /**
     * 停止HTTP服务器
     */
    fun stop() {
        if (app == null) {
            logger.warn("HTTP服务器未运行")
            return
        }

        try {
            logger.info("正在停止HTTP服务器...")
            app?.stop()
            app = null
            logger.info("HTTP服务器已停止")
        } catch (e: Exception) {
            logger.error("停止HTTP服务器时发生错误", e)
        }
    }

    /**
     * 注册所有REST API路由
     */
    private fun registerRoutes() {
        val javalin = app ?: return

        // GET /ping - 健康检查
        javalin.get("/ping") { ctx ->
            handlePing(ctx)
        }

        // GET /manifest.json - 获取资源包清单
        javalin.get("/manifest.json") { ctx ->
            handleGetManifest(ctx)
        }

        // GET /packs/{filename} - 下载资源包文件
        javalin.get("/packs/{filename}") { ctx ->
            handleDownloadPack(ctx)
        }

        // 404处理
        javalin.error(404) { ctx ->
            ctx.json(mapOf(
                "error" to "Not Found",
                "message" to "请求的资源不存在: ${ctx.path()}"
            ))
        }

        // 500处理
        javalin.error(500) { ctx ->
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "服务器内部错误"
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
     * 处理获取清单请求
     * GET /manifest.json
     */
    private fun handleGetManifest(ctx: Context) {
        try {
            logger.info("生成资源包清单...")
            val manifest = manifestGenerator.generate()

            // 返回JSON响应
            ctx.contentType("application/json")
            ctx.result(gson.toJson(manifest))

            logger.info("清单生成成功: ${manifest.getPackCount()} 个资源包")
        } catch (e: Exception) {
            logger.error("生成清单失败", e)
            ctx.status(500)
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "生成资源包清单失败: ${e.message}"
            ))
        }
    }

    /**
     * 处理下载资源包请求
     * GET /packs/{filename}
     */
    private fun handleDownloadPack(ctx: Context) {
        val filename = ctx.pathParam("filename")

        // 安全检查：防止路径遍历攻击
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("检测到非法文件名: $filename")
            ctx.status(400)
            ctx.json(mapOf(
                "error" to "Bad Request",
                "message" to "非法的文件名"
            ))
            return
        }

        // 优先在缓存目录查找文件夹包
        val cacheDir = File(packDirectory, ".cache")
        var file = File(cacheDir, filename)

        // 如果缓存中没有，查找主目录中的原始ZIP/MCPACK
        if (!file.exists() || !file.isFile) {
            file = File(packDirectory, filename)
        }

        // 检查文件是否存在
        if (!file.exists() || !file.isFile) {
            logger.warn("请求的文件不存在: $filename")
            ctx.status(404)
            ctx.json(mapOf(
                "error" to "Not Found",
                "message" to "资源包文件不存在: $filename"
            ))
            return
        }

        // 检查文件类型（只允许.zip和.mcpack）
        if (!filename.endsWith(".zip") && !filename.endsWith(".mcpack")) {
            logger.warn("请求的文件类型不允许: $filename")
            ctx.status(403)
            ctx.json(mapOf(
                "error" to "Forbidden",
                "message" to "不允许下载该类型的文件"
            ))
            return
        }

        try {
            // 传输文件
            logger.info("开始传输文件: $filename (${file.length()} 字节)")

            ctx.contentType("application/octet-stream")
            ctx.header("Content-Disposition", "attachment; filename=\"$filename\"")

            // 对于资源包文件（通常几MB），直接读取到内存是安全的
            // 这避免了流生命周期管理的问题
            ctx.result(file.readBytes())

            logger.info("文件传输完成: $filename")
        } catch (e: Exception) {
            logger.error("传输文件失败: $filename", e)
            ctx.status(500)
            ctx.json(mapOf(
                "error" to "Internal Server Error",
                "message" to "文件传输失败: ${e.message}"
            ))
        }
    }

    /**
     * 检查服务器是否在运行
     */
    fun isRunning(): Boolean {
        return app != null
    }
}
