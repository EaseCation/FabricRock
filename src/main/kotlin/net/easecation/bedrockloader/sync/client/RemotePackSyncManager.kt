package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.sync.client.listener.SyncListener
import net.easecation.bedrockloader.sync.client.model.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 远程资源包同步管理器
 * 核心类，协调所有同步组件
 */
class RemotePackSyncManager(
    private val packDirectory: File,
    private val configFile: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/RemoteSync")
    private val listeners = CopyOnWriteArrayList<SyncListener>()

    @Volatile
    private var currentState: SyncState = SyncState.Idle

    @Volatile
    private var cancelled = false

    // ==================== 监听器管理 ====================

    /**
     * 添加同步监听器
     */
    fun addListener(listener: SyncListener) {
        listeners.add(listener)
        logger.debug("添加监听器: ${listener.javaClass.simpleName}")
    }

    /**
     * 移除同步监听器
     */
    fun removeListener(listener: SyncListener) {
        listeners.remove(listener)
        logger.debug("移除监听器: ${listener.javaClass.simpleName}")
    }

    /**
     * 清空所有监听器
     */
    fun clearListeners() {
        listeners.clear()
        logger.debug("清空所有监听器")
    }

    // ==================== 核心同步方法（阶段2实现） ====================

    /**
     * 检查资源包更新
     * 阶段2只检查和对比，不下载文件
     *
     * @return 同步检查结果
     */
    fun checkForUpdates(): SyncResult {
        logger.info("========== 开始检查资源包更新 ==========")
        setState(SyncState.LoadingConfig)
        notifyListeners { it.onCheckingStart() }

        try {
            // 1. 加载配置
            checkCancelled()
            logger.debug("正在加载客户端配置...")
            notifyListeners { it.onCheckingProgress("正在加载配置...") }

            val config = ClientConfigLoader.loadClientConfig(configFile)

            if (!config.enabled) {
                logger.info("远程同步已禁用（配置: enabled=false）")
                setState(SyncState.Complete)
                return SyncResult.Disabled
            }

            logger.info("服务器地址: ${config.serverUrl}")
            logger.info("超时时间: ${config.timeoutSeconds}秒")

            // 2. 获取远程manifest
            checkCancelled()
            setState(SyncState.FetchingManifest)
            logger.debug("正在连接服务器...")
            notifyListeners { it.onCheckingProgress("正在连接服务器: ${config.serverUrl}") }

            val client = ManifestClient(config)
            val manifest = client.fetchManifest()

            logger.info("成功获取manifest:")
            logger.info("  - 版本: ${manifest.version}")
            logger.info("  - 生成时间: ${java.util.Date(manifest.generatedAt)}")
            logger.info("  - 远程包数量: ${manifest.packs.size}")

            // 3. 对比本地包
            checkCancelled()
            setState(SyncState.ComparingPacks)
            logger.debug("正在对比remote/目录中的资源包...")
            notifyListeners { it.onCheckingProgress("正在对比本地资源包...") }

            val comparator = PackComparator(packDirectory, config)
            val plan = comparator.compare(manifest)

            logger.info("对比完成:")
            logger.info("  - 需要下载新包: ${plan.toDownload.size}")
            logger.info("  - 需要更新包: ${plan.toUpdate.size}")
            logger.info("  - 已是最新: ${plan.upToDate.size}")
            logger.info("  - 仅remote/目录存在: ${plan.localOnly.size}")
            logger.info("  - 待清理: ${plan.packagesToCleanup.size}")

            if (plan.hasDownloads) {
                logger.info("  - 总共需要下载: ${plan.totalToDownload} 个文件，${formatBytes(plan.totalBytes)}")
            }

            // 4. 返回结果
            setState(SyncState.PlanReady(plan))
            notifyListeners { it.onCheckingComplete(plan) }

            logger.info("========== 检查完成 ==========")
            return SyncResult.Success(plan)

        } catch (e: CancelledException) {
            logger.warn("检查已取消")
            setState(SyncState.Cancelled)
            notifyListeners { it.onCancelled("用户取消了检查") }
            return SyncResult.Cancelled

        } catch (e: SyncError.NetworkError) {
            logger.warn("网络错误: ${e.message}")
            val offline = SyncResult.Offline("无法连接到服务器，将使用本地资源包")
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return offline

        } catch (e: SyncError) {
            logger.error("同步错误: ${e.message}", e.cause)
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return SyncResult.Failed(e)

        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            logger.error("检查失败: ${error.message}", e)
            setState(SyncState.Error(error))
            notifyListeners { it.onError(error) }
            return SyncResult.Failed(error)
        }
    }

    // ==================== 下载方法（阶段4实现） ====================

    /**
     * 下载资源包
     * 阶段4实现：从服务器下载所有需要的资源包文件
     *
     * @param plan 同步计划
     * @return 下载结果
     */
    fun downloadPacks(plan: SyncPlan): DownloadResult {
        logger.info("========== 开始下载资源包 ==========")

        // 检查是否有需要下载的文件
        if (!plan.hasDownloads) {
            logger.info("没有需要下载的文件")
            setState(SyncState.Complete)
            notifyListeners { it.onDownloadComplete(0, 0) }
            return DownloadResult.Success(emptyList(), emptyList())
        }

        // 设置为下载中状态
        setState(SyncState.Downloading(0, plan.totalToDownload, 0f))

        try {
            // 1. 加载配置
            checkCancelled()
            val config = ClientConfigLoader.loadClientConfig(configFile)

            // 2. 创建下载器
            val downloader = PackDownloader(config, packDirectory)

            // 3. 清理旧的临时文件
            downloader.cleanupTempFiles()

            // 4. 合并待下载列表
            val packList = plan.toDownload + plan.toUpdate
            logger.info("准备下载 ${packList.size} 个文件，总大小: ${formatBytes(plan.totalBytes)}")

            // 5. 通知开始下载
            notifyListeners { it.onDownloadStart(plan.totalToDownload, plan.totalBytes) }

            // 6. 执行批量下载
            val result = downloader.downloadAll(
                packs = packList,
                onProgress = { index, total ->
                    logger.debug("总体进度: $index/$total")
                },
                onFileStart = { file, index, total ->
                    notifyListeners { it.onFileDownloadStart(file, index, total) }
                },
                onFileProgress = { file, bytesDownloaded, totalBytes ->
                    notifyListeners { it.onFileProgress(file, bytesDownloaded, totalBytes) }
                },
                onFileComplete = { file ->
                    notifyListeners { it.onFileDownloadComplete(file) }
                },
                isCancelled = { cancelled }
            )

            // 7. 清理远程已删除的包
            if (plan.packagesToCleanup.isNotEmpty()) {
                logger.info("========== 开始清理远程已删除的包 ==========")
                cleanupRemovedPacks(plan.packagesToCleanup)
                logger.info("========== 清理完成 ==========")
            }

            // 8. 处理下载结果
            setState(SyncState.Complete)
            notifyListeners { it.onDownloadComplete(result.successCount, result.failCount) }

            logger.info("========== 下载完成 ==========")
            logger.info("成功: ${result.successCount}, 失败: ${result.failCount}")

            return if (result.failCount == 0) {
                DownloadResult.Success(result.successFiles, emptyList())
            } else {
                DownloadResult.Success(result.successFiles, result.failedFiles)
            }

        } catch (e: CancelledException) {
            logger.warn("下载已取消")
            setState(SyncState.Cancelled)
            notifyListeners { it.onCancelled("用户取消了下载") }
            return DownloadResult.Cancelled

        } catch (e: SyncError) {
            logger.error("下载失败: ${e.message}", e.cause)
            setState(SyncState.Error(e))
            notifyListeners { it.onError(e) }
            return DownloadResult.Failed(e)

        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            logger.error("下载失败: ${error.message}", e)
            setState(SyncState.Error(error))
            notifyListeners { it.onError(error) }
            return DownloadResult.Failed(error)
        }
    }

    // ==================== 控制方法 ====================

    /**
     * 取消当前同步操作
     */
    fun cancel() {
        if (!cancelled) {
            logger.info("收到取消请求")
            cancelled = true
        }
    }

    /**
     * 重置同步管理器状态
     */
    fun reset() {
        logger.debug("重置同步管理器")
        cancelled = false
        setState(SyncState.Idle)
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): SyncState = currentState

    /**
     * 是否已取消
     */
    fun isCancelled(): Boolean = cancelled

    // ==================== 内部方法 ====================

    /**
     * 设置状态
     */
    private fun setState(newState: SyncState) {
        val oldState = currentState
        currentState = newState
        logger.debug("状态变化: $oldState -> $newState")
    }

    /**
     * 通知所有监听器
     */
    private fun notifyListeners(action: (SyncListener) -> Unit) {
        listeners.forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                logger.error("监听器执行失败: ${listener.javaClass.simpleName}", e)
            }
        }
    }

    /**
     * 检查是否已取消
     * @throws CancelledException 如果已取消
     */
    private fun checkCancelled() {
        if (cancelled) {
            throw CancelledException("检查已被取消")
        }
    }

    /**
     * 将异常映射为SyncError
     */
    private fun mapExceptionToError(e: Exception): SyncError {
        return when (e) {
            is SyncError -> e
            is ConnectException -> SyncError.NetworkError("无法连接到服务器", e)
            is java.net.SocketTimeoutException -> SyncError.NetworkError("连接超时", e)
            is IOException -> SyncError.NetworkError("网络IO错误", e)
            else -> SyncError.UnknownError(e.message ?: "未知错误", e)
        }
    }

    /**
     * 清理远程已删除的包
     * 删除remote/目录中那些远程服务器已删除的包
     *
     * @param filenames 要清理的文件名列表
     */
    private fun cleanupRemovedPacks(filenames: List<String>) {
        if (filenames.isEmpty()) {
            return
        }

        logger.info("准备清理 ${filenames.size} 个远程已删除的包")
        val remoteDirectory = File(packDirectory, "remote")

        var cleanedCount = 0
        var failedCount = 0

        for (filename in filenames) {
            val file = File(remoteDirectory, filename)
            if (file.exists()) {
                logger.info("清理包: $filename")
                if (file.delete()) {
                    cleanedCount++
                    logger.debug("  成功删除: ${file.absolutePath}")
                } else {
                    failedCount++
                    logger.warn("  删除失败: ${file.absolutePath}")
                }
            } else {
                logger.warn("文件不存在，跳过: $filename")
            }
        }

        logger.info("清理完成 - 成功: $cleanedCount, 失败: $failedCount")
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
