package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.sync.client.listener.ConsoleSyncListener
import net.easecation.bedrockloader.sync.client.listener.SyncListener
import net.easecation.bedrockloader.sync.client.model.DownloadResult
import net.easecation.bedrockloader.sync.client.model.SyncResult
import net.easecation.bedrockloader.sync.client.ui.DownloadWindow
import net.easecation.bedrockloader.sync.client.ui.SwingSyncListener
import net.easecation.bedrockloader.sync.client.ui.UIUtil
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 远程资源包同步 PreLaunch 入口点
 * 在游戏启动前执行，检查并同步资源包
 * 注意：仅在客户端环境执行
 */
class RemotePackSyncPreLaunch : PreLaunchEntrypoint {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PreLaunch")

    override fun onPreLaunch() {
        // 检查环境类型，只在客户端执行
        val envType = FabricLoader.getInstance().environmentType
        if (envType != EnvType.CLIENT) {
            logger.debug("当前环境为服务端，跳过客户端资源包同步")
            return
        }

        logger.info("========================================")
        logger.info("Bedrock Loader - Remote Pack Sync")
        logger.info("阶段3: PreLaunch同步与UI集成")
        logger.info("========================================")

        try {
            // 获取游戏目录
            val gameDir = FabricLoader.getInstance().gameDir.toFile()
            val packDirectory = File(gameDir, "config/bedrock-loader")
            val configFile = File(packDirectory, "client.yml")

            logger.info("游戏目录: ${gameDir.absolutePath}")
            logger.info("资源包目录: ${packDirectory.absolutePath}")
            logger.info("配置文件: ${configFile.absolutePath}")

            // 加载配置
            val config = ClientConfigLoader.loadClientConfig(configFile)
            logger.info("配置加载完成 - showUI: ${config.showUI}, enabled: ${config.enabled}")

            // 如果禁用，直接返回
            if (!config.enabled) {
                logger.info("远程同步已禁用（配置: enabled=false）")
                logger.info("将使用本地资源包继续启动游戏")
                return
            }

            // 创建同步管理器
            val syncManager = RemotePackSyncManager(
                packDirectory = packDirectory,
                configFile = configFile
            )

            // 根据配置选择监听器
            val (listener, window) = createListener(config, syncManager)
            syncManager.addListener(listener)

            // 显示UI窗口（如果启用）
            window?.isVisible = true

            // 执行同步检查
            logger.debug("开始执行同步检查...")
            val checkResult = syncManager.checkForUpdates()

            // 处理检查结果
            when (checkResult) {
                is SyncResult.Success -> {
                    val plan = checkResult.plan
                    if (plan.isUpToDate) {
                        logger.info("所有资源包已是最新，无需下载")
                        // 窗口自己管理关闭，不需要手动close
                    } else {
                        logger.info("发现 ${plan.totalToDownload} 个需要下载的资源包")

                        // 执行下载（阶段4实现）
                        logger.debug("调用 downloadPacks()...")
                        val downloadResult = syncManager.downloadPacks(plan)

                        // 处理下载结果
                        when (downloadResult) {
                            is DownloadResult.Success -> {
                                logger.info("下载完成 - 成功: ${downloadResult.downloadedFiles.size}, 失败: ${downloadResult.failedFiles.size}")
                                // 窗口自己管理关闭
                            }
                            is DownloadResult.Failed -> {
                                logger.error("下载失败: ${downloadResult.error.message}")
                                // 错误对话框已在监听器中显示
                            }
                            is DownloadResult.Cancelled -> {
                                logger.warn("下载已取消")
                                // 窗口自己管理关闭
                            }
                        }
                    }
                }

                is SyncResult.Disabled -> {
                    logger.info("远程同步已禁用，将使用本地资源包")
                    // 窗口自己管理关闭
                }

                is SyncResult.Offline -> {
                    logger.warn("服务器不可用: ${checkResult.message}")
                    logger.info("将使用本地资源包继续启动游戏")
                    // 窗口自己管理关闭
                }

                is SyncResult.Cancelled -> {
                    logger.warn("同步检查已取消")
                    // 窗口自己管理关闭
                }

                is SyncResult.Failed -> {
                    logger.error("同步检查失败: ${checkResult.error.message}")
                    logger.info("将使用本地资源包继续启动游戏")
                    // 错误对话框已在监听器中显示
                }
            }

        } catch (e: Exception) {
            logger.error("PreLaunch同步检查发生异常", e)
            logger.info("将忽略错误并使用本地资源包继续启动游戏")
        }

        logger.info("========================================")
        logger.info("PreLaunch同步检查完成")
        logger.info("游戏将继续启动...")
        logger.info("========================================")
    }

    /**
     * 根据配置创建监听器
     *
     * @param config 客户端配置
     * @param syncManager 同步管理器
     * @return Pair(监听器, 窗口) - 如果是控制台模式，窗口为null
     */
    private fun createListener(
        config: ClientConfig,
        syncManager: RemotePackSyncManager
    ): Pair<SyncListener, DownloadWindow?> {
        return if (config.showUI) {
            logger.info("启用UI模式 - 创建Swing窗口")

            // 设置macOS兼容性
            UIUtil.ensureMacOSCompatibility()

            // 创建下载窗口
            val window = DownloadWindow(
                onCancel = {
                    logger.info("用户点击取消按钮")
                    syncManager.cancel()
                }
            )

            // 创建Swing监听器
            val listener = SwingSyncListener(window)

            Pair(listener, window)
        } else {
            logger.info("启用控制台模式 - 使用ConsoleSyncListener")
            Pair(ConsoleSyncListener(), null)
        }
    }
}
