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
 * Remote Pack Sync PreLaunch Entry Point
 * Executes before game launch to check and sync resource packs
 * Note: Only runs in client environment
 */
class RemotePackSyncPreLaunch : PreLaunchEntrypoint {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PreLaunch")

    override fun onPreLaunch() {
        // Check environment type, only run on client
        val envType = FabricLoader.getInstance().environmentType
        if (envType != EnvType.CLIENT) {
            return
        }

        logger.info("BedrockLoader - Remote Pack Sync")

        try {
            // Get game directory
            val gameDir = FabricLoader.getInstance().gameDir.toFile()
            val packDirectory = File(gameDir, "config/bedrock-loader")
            val configFile = File(packDirectory, "client.yml")

            // Load config
            val config = ClientConfigLoader.loadClientConfig(configFile)

            // If disabled, return directly
            if (!config.enabled) {
                logger.info("Remote sync disabled, using local packs")
                return
            }

            // Create sync manager
            val syncManager = RemotePackSyncManager(
                packDirectory = packDirectory,
                configFile = configFile
            )

            // Select listener based on config
            val (listener, window) = createListener(config, syncManager)
            syncManager.addListener(listener)

            // Show UI window if enabled
            window?.isVisible = true
            window?.bringToFront()

            // Execute sync check
            val checkResult = syncManager.checkForUpdates()

            // Process check result
            when (checkResult) {
                is SyncResult.Success -> {
                    val plan = checkResult.plan
                    if (plan.isUpToDate) {
                        logger.info("All packs are up to date")
                    } else {
                        logger.info("Found ${plan.totalToDownload} pack(s) to download")

                        // Execute download
                        val downloadResult = syncManager.downloadPacks(plan)

                        // Process download result
                        when (downloadResult) {
                            is DownloadResult.Success -> {
                                logger.info("Download complete: ${downloadResult.downloadedFiles.size} succeeded, ${downloadResult.failedFiles.size} failed")
                            }
                            is DownloadResult.Failed -> {
                                logger.error("Download failed: ${downloadResult.error.message}")
                            }
                            is DownloadResult.Cancelled -> {
                                logger.warn("Download cancelled")
                            }
                        }
                    }
                }

                is SyncResult.Disabled -> {
                    logger.info("Remote sync disabled, using local packs")
                }

                is SyncResult.Offline -> {
                    logger.warn("Server unavailable: ${checkResult.message}")
                    logger.info("Using local packs")
                }

                is SyncResult.Cancelled -> {
                    logger.warn("Sync check cancelled")
                }

                is SyncResult.Failed -> {
                    logger.error("Sync check failed: ${checkResult.error.message}")
                    logger.info("Using local packs")
                }
            }

        } catch (e: Exception) {
            logger.error("PreLaunch sync check error", e)
            logger.info("Ignoring error and using local packs")
        }

        logger.info("PreLaunch sync check complete, game will continue to start")
    }

    /**
     * Create listener based on config
     *
     * @param config Client config
     * @param syncManager Sync manager
     * @return Pair(listener, window) - If console mode, window is null
     */
    private fun createListener(
        config: ClientConfig,
        syncManager: RemotePackSyncManager
    ): Pair<SyncListener, DownloadWindow?> {
        return if (config.showUI) {
            logger.info("Using UI mode - creating Swing window")

            // Set macOS compatibility
            UIUtil.ensureMacOSCompatibility()

            // Create download window
            val window = DownloadWindow(
                onCancel = {
                    logger.info("User clicked cancel button")
                    syncManager.cancel()
                }
            )

            // Create Swing listener
            val listener = SwingSyncListener(window)

            Pair(listener, window)
        } else {
            logger.info("Using console mode")
            Pair(ConsoleSyncListener(), null)
        }
    }
}
