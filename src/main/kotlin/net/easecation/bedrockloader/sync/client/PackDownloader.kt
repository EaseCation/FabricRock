package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.sync.client.model.SyncError
import net.easecation.bedrockloader.sync.common.MD5Util
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 资源包下载器
 * 负责从HTTP服务器下载资源包文件到remote/子目录
 */
class PackDownloader(
    private val config: ClientConfig,
    private val packDirectory: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PackDownloader")

    /**
     * 远程包存储目录（packDirectory/remote）
     */
    private val remoteDirectory = File(packDirectory, "remote").apply {
        if (!exists()) {
            logger.info("创建remote目录: $absolutePath")
            mkdirs()
        }
    }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
        .build()

    /**
     * 下载单个文件结果
     */
    sealed class DownloadFileResult {
        data class Success(val file: File) : DownloadFileResult()
        data class Failed(val error: SyncError) : DownloadFileResult()
    }

    /**
     * 批量下载结果
     */
    data class BatchDownloadResult(
        val successFiles: List<String>,
        val failedFiles: List<String>,
        val successCount: Int,
        val failCount: Int
    )

    /**
     * 清理remote/目录中的临时文件
     * 清理所有.downloading和.backup后缀的文件
     */
    fun cleanupTempFiles() {
        logger.debug("清理remote/目录中的临时文件...")
        var cleanedCount = 0

        remoteDirectory.listFiles()?.forEach { file ->
            if (file.name.endsWith(".downloading") || file.name.endsWith(".backup")) {
                logger.info("清理临时文件: ${file.name}")
                if (file.delete()) {
                    cleanedCount++
                } else {
                    logger.warn("无法删除临时文件: ${file.name}")
                }
            }
        }

        if (cleanedCount > 0) {
            logger.info("已清理 $cleanedCount 个临时文件")
        } else {
            logger.debug("没有需要清理的临时文件")
        }
    }

    /**
     * 下载单个文件（带重试机制）
     *
     * @param pack 资源包信息
     * @param onProgress 进度回调 (bytesDownloaded, totalBytes)
     * @param isCancelled 检查是否取消的函数
     * @param maxRetries 最大重试次数
     * @return 下载结果
     */
    fun downloadFile(
        pack: RemotePackInfo,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean,
        maxRetries: Int = 3
    ): DownloadFileResult {
        logger.info("准备下载: ${pack.name} (${formatBytes(pack.size)})")

        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                // 1. 下载到临时文件
                val tempFile = downloadToTempFile(pack, onProgress, isCancelled)

                // 2. 验证MD5并替换
                verifyMD5AndReplace(tempFile, pack)

                logger.info("文件下载成功: ${pack.name}")
                return DownloadFileResult.Success(File(remoteDirectory, pack.name))

            } catch (e: CancelledException) {
                // 用户取消，不重试
                logger.warn("下载已取消: ${pack.name}")
                throw e
            } catch (e: Exception) {
                lastError = e
                logger.warn("下载失败 (尝试 ${attempt + 1}/$maxRetries): ${pack.name} - ${e.message}")

                if (attempt < maxRetries - 1) {
                    logger.debug("等待1秒后重试...")
                    Thread.sleep(1000)
                }
            }
        }

        // 所有重试都失败
        val errorMessage = "下载失败，已重试${maxRetries}次: ${lastError?.message}"
        logger.error(errorMessage)
        return DownloadFileResult.Failed(
            SyncError.FileError(pack.name, errorMessage, lastError)
        )
    }

    /**
     * 批量下载所有文件
     *
     * @param packs 要下载的资源包列表
     * @param onProgress 总体进度回调 (currentIndex, total)
     * @param onFileStart 文件开始下载回调 (file, index, total)
     * @param onFileProgress 单个文件进度回调 (file, bytesDownloaded, totalBytes)
     * @param onFileComplete 单个文件完成回调 (file)
     * @param isCancelled 检查是否取消的函数
     * @return 批量下载结果
     */
    fun downloadAll(
        packs: List<RemotePackInfo>,
        onProgress: (Int, Int) -> Unit,
        onFileStart: (RemotePackInfo, Int, Int) -> Unit,
        onFileProgress: (RemotePackInfo, Long, Long) -> Unit,
        onFileComplete: (RemotePackInfo) -> Unit,
        isCancelled: () -> Boolean
    ): BatchDownloadResult {
        logger.info("开始批量下载 ${packs.size} 个文件")

        val successFiles = mutableListOf<String>()
        val failedFiles = mutableListOf<String>()

        packs.forEachIndexed { index, pack ->
            // 检查取消状态
            if (isCancelled()) {
                logger.warn("批量下载已取消")
                throw CancelledException("用户取消了下载")
            }

            // 通知总体进度
            onProgress(index + 1, packs.size)

            // 通知文件开始下载
            onFileStart(pack, index + 1, packs.size)

            // 下载单个文件
            val result = downloadFile(
                pack = pack,
                onProgress = { downloaded, total ->
                    onFileProgress(pack, downloaded, total)
                },
                isCancelled = isCancelled
            )

            // 处理结果
            when (result) {
                is DownloadFileResult.Success -> {
                    successFiles.add(pack.name)
                    onFileComplete(pack)
                    logger.info("进度: ${index + 1}/${packs.size} - ${pack.name} 完成")
                }
                is DownloadFileResult.Failed -> {
                    failedFiles.add(pack.name)
                    logger.error("进度: ${index + 1}/${packs.size} - ${pack.name} 失败")

                    // 如果配置为出错时自动取消，则抛出异常
                    if (config.autoCancelOnError) {
                        throw result.error
                    }
                }
            }
        }

        val batchResult = BatchDownloadResult(
            successFiles = successFiles,
            failedFiles = failedFiles,
            successCount = successFiles.size,
            failCount = failedFiles.size
        )

        logger.info("批量下载完成 - 成功: ${batchResult.successCount}, 失败: ${batchResult.failCount}")
        return batchResult
    }

    /**
     * 下载文件到临时文件
     *
     * @param pack 资源包信息
     * @param onProgress 进度回调
     * @param isCancelled 检查是否取消
     * @return 下载的临时文件
     */
    private fun downloadToTempFile(
        pack: RemotePackInfo,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ): File {
        // 构建下载URL
        val downloadUrl = "${config.serverUrl.trimEnd('/')}${pack.url}"
        logger.debug("下载URL: $downloadUrl")

        // 创建remote/目录中的临时文件
        val tempFile = File(remoteDirectory, "${pack.name}.downloading")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            // 创建HTTP请求
            val request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .timeout(Duration.ofSeconds(config.timeoutSeconds.toLong()))
                .build()

            // 发送请求并获取响应
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

            // 检查HTTP状态码
            when (response.statusCode()) {
                200 -> {
                    logger.debug("开始接收数据...")

                    // 流式写入文件
                    FileOutputStream(tempFile).use { output ->
                        response.body().use { input ->
                            val buffer = ByteArray(8192) // 8KB 缓冲区
                            var totalBytesRead = 0L
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                // 检查取消状态
                                if (isCancelled()) {
                                    logger.warn("下载被用户取消")
                                    tempFile.delete()
                                    throw CancelledException("用户取消了下载")
                                }

                                // 写入数据
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                // 回调进度
                                onProgress(totalBytesRead, pack.size)
                            }

                            logger.debug("下载完成，共 ${formatBytes(totalBytesRead)}")
                        }
                    }

                    return tempFile
                }
                404 -> {
                    throw SyncError.ServerError(404, "文件不存在: ${pack.url}")
                }
                else -> {
                    throw SyncError.ServerError(response.statusCode(), "服务器返回错误: HTTP ${response.statusCode()}")
                }
            }
        } catch (e: ConnectException) {
            tempFile.delete()
            throw SyncError.NetworkError("无法连接到服务器: ${config.serverUrl}", e)
        } catch (e: java.net.SocketTimeoutException) {
            tempFile.delete()
            throw SyncError.NetworkError("下载超时 (${config.timeoutSeconds}秒)", e)
        } catch (e: SyncError) {
            tempFile.delete()
            throw e
        } catch (e: CancelledException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            throw SyncError.FileError(pack.name, "下载失败: ${e.message}", e)
        }
    }

    /**
     * 验证MD5并替换文件
     *
     * @param tempFile 临时文件
     * @param pack 资源包信息
     */
    private fun verifyMD5AndReplace(tempFile: File, pack: RemotePackInfo) {
        logger.debug("验证MD5: ${pack.name}")

        // 计算文件的MD5
        val actualMD5 = try {
            MD5Util.calculateMD5(tempFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw SyncError.FileError(pack.name, "计算MD5失败: ${e.message}", e)
        }

        // 验证MD5是否匹配
        if (!actualMD5.equals(pack.md5, ignoreCase = true)) {
            logger.error("MD5验证失败 - 期望: ${pack.md5}, 实际: $actualMD5")
            tempFile.delete()
            throw SyncError.FileError(
                pack.name,
                "MD5验证失败 (期望: ${pack.md5}, 实际: $actualMD5)"
            )
        }

        logger.debug("MD5验证通过")

        // 原子性替换文件（在remote/目录中）
        val targetFile = File(remoteDirectory, pack.name)
        atomicReplace(targetFile, tempFile)
    }

    /**
     * 原子性文件替换
     * 使用备份和恢复机制确保替换的原子性
     *
     * @param targetFile 目标文件（在remote/目录中）
     * @param newFile 新文件（临时文件）
     */
    private fun atomicReplace(targetFile: File, newFile: File) {
        val backup = File(remoteDirectory, "${targetFile.name}.backup")

        try {
            // 1. 如果目标文件存在，先备份
            if (targetFile.exists()) {
                logger.debug("备份旧文件: ${targetFile.name}")
                if (!targetFile.renameTo(backup)) {
                    throw IOException("无法备份旧文件")
                }
            }

            // 2. 移动新文件到目标位置
            logger.debug("替换文件: ${targetFile.name}")
            if (!newFile.renameTo(targetFile)) {
                // 替换失败，恢复备份
                if (backup.exists()) {
                    backup.renameTo(targetFile)
                }
                throw IOException("无法替换文件")
            }

            // 3. 删除备份
            if (backup.exists()) {
                backup.delete()
                logger.debug("已删除备份文件")
            }

            logger.info("文件替换成功: ${targetFile.name}")
        } catch (e: Exception) {
            // 失败时尝试恢复备份
            if (backup.exists() && !targetFile.exists()) {
                logger.warn("替换失败，正在恢复备份...")
                backup.renameTo(targetFile)
            }
            throw SyncError.FileError(targetFile.name, "文件替换失败: ${e.message}", e)
        }
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

/**
 * 取消异常
 * 用于表示下载被用户取消
 */
class CancelledException(message: String = "操作已取消") : Exception(message)
