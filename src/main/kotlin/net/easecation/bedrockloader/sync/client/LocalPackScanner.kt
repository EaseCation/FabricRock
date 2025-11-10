package net.easecation.bedrockloader.sync.client

import com.google.gson.reflect.TypeToken
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.sync.common.MD5Util
import net.easecation.bedrockloader.util.GsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

/**
 * 本地资源包扫描器
 * 扫描本地的资源包文件（包括手动放置的包和remote/目录中远程下载的包）
 * 提取包的UUID、版本信息并计算MD5，用于同步比较和冲突检测
 */
class LocalPackScanner(
    private val packDirectory: File
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/LocalPackScanner")

    /**
     * 远程包存储目录（packDirectory/remote）
     */
    private val remoteDirectory = File(packDirectory, "remote")

    /**
     * 本地资源包信息
     */
    data class LocalPackInfo(
        val filename: String,
        val uuid: String?,      // 包的UUID（从manifest.json的header.uuid提取）
        val version: String?,   // 包的版本（从manifest.json的header.version提取）
        val md5: String,
        val size: Long,
        val file: File,
        val isManual: Boolean   // 是否为手动放置的包（非remote/目录）
    )

    /**
     * 扫描所有本地资源包（手动放置的包 + remote/目录中的包）
     *
     * @return 本地资源包信息列表（按文件名排序）
     */
    fun scan(): List<LocalPackInfo> {
        val allPacks = mutableListOf<LocalPackInfo>()

        // 1. 扫描手动放置的包（根目录，排除remote/和.cache/）
        logger.debug("扫描手动放置的资源包: ${packDirectory.absolutePath}")
        val manualPacks = scanDirectory(packDirectory, isManual = true, excludeDirs = setOf("remote", ".cache"))
        logger.info("找到 ${manualPacks.size} 个手动放置的资源包")
        allPacks.addAll(manualPacks)

        // 2. 扫描remote/目录中远程下载的包
        if (remoteDirectory.exists()) {
            if (remoteDirectory.isDirectory) {
                logger.debug("扫描远程下载的资源包: ${remoteDirectory.absolutePath}")
                val remotePacks = scanDirectory(remoteDirectory, isManual = false, excludeDirs = emptySet())
                logger.info("找到 ${remotePacks.size} 个远程下载的资源包")
                allPacks.addAll(remotePacks)
            } else {
                logger.error("remote路径不是目录: ${remoteDirectory.absolutePath}")
            }
        } else {
            logger.info("remote目录不存在，创建: ${remoteDirectory.absolutePath}")
            remoteDirectory.mkdirs()
        }

        logger.info("总共找到 ${allPacks.size} 个本地资源包")
        return allPacks.sortedBy { it.filename }
    }

    /**
     * 扫描指定目录中的资源包文件
     *
     * @param directory 要扫描的目录
     * @param isManual 是否为手动放置的包
     * @param excludeDirs 要排除的子目录名称
     * @return 资源包信息列表
     */
    private fun scanDirectory(directory: File, isManual: Boolean, excludeDirs: Set<String>): List<LocalPackInfo> {
        val packFiles = directory.listFiles { dir, name ->
            // 创建子文件/目录对象
            val child = File(dir, name)

            // 排除子目录
            if (child.isDirectory && excludeDirs.contains(name)) {
                return@listFiles false
            }
            // 只扫描文件
            isValidPackFile(child, name)
        }?.toList() ?: emptyList()

        return packFiles.mapNotNull { file ->
            try {
                scanPackFile(file, isManual)
            } catch (e: Exception) {
                logger.error("扫描文件失败: ${file.name}", e)
                null
            }
        }
    }

    /**
     * 扫描单个资源包文件
     *
     * @param file 资源包文件
     * @param isManual 是否为手动放置的包
     * @return 资源包信息
     */
    private fun scanPackFile(file: File, isManual: Boolean): LocalPackInfo {
        logger.debug("扫描文件: ${file.name} (${if (isManual) "手动" else "远程"})")

        // 提取UUID和版本信息
        val (uuid, version) = extractPackMetadata(file)

        val md5 = MD5Util.calculateMD5(file)
        val size = file.length()

        logger.debug("  - UUID: ${uuid ?: "无法提取"}")
        logger.debug("  - 版本: ${version ?: "无法提取"}")
        logger.debug("  - MD5: $md5")
        logger.debug("  - 大小: $size 字节")

        return LocalPackInfo(
            filename = file.name,
            uuid = uuid,
            version = version,
            md5 = md5,
            size = size,
            file = file,
            isManual = isManual
        )
    }

    /**
     * 从资源包ZIP文件中提取UUID和版本信息
     *
     * @param file 资源包文件
     * @return Pair(UUID, Version)，如果提取失败则返回(null, null)
     */
    private fun extractPackMetadata(file: File): Pair<String?, String?> {
        try {
            ZipFile(file).use { zip ->
                // 查找manifest.json（支持两种可能的名称）
                val entry = zip.getEntry("manifest.json") ?: zip.getEntry("pack_manifest.json")
                if (entry == null) {
                    logger.warn("文件 ${file.name} 中找不到manifest.json")
                    return Pair(null, null)
                }

                // 使用GSON反序列化
                val type = object : TypeToken<PackManifest>() {}.type
                val manifest: PackManifest = GsonUtil.GSON.fromJson(
                    InputStreamReader(zip.getInputStream(entry)),
                    type
                )

                // 提取header.uuid和header.version
                val uuid = manifest.header?.uuid?.toString()
                val version = manifest.header?.version?.toString()

                return Pair(uuid, version)
            }
        } catch (e: Exception) {
            logger.warn("提取 ${file.name} 的metadata失败: ${e.message}")
            return Pair(null, null)
        }
    }

    /**
     * 判断文件是否为有效的资源包文件
     * 复用服务端PackScanner的逻辑
     */
    private fun isValidPackFile(file: File, name: String): Boolean {
        return file.isFile
                && (name.endsWith(".zip") || name.endsWith(".mcpack"))
                && !name.startsWith(".")
                && !name.endsWith(".downloading")
                && !name.endsWith(".tmp")
    }

    /**
     * 根据文件名查找本地包信息
     */
    fun findByFilename(filename: String, localPacks: List<LocalPackInfo>): LocalPackInfo? {
        return localPacks.find { it.filename == filename }
    }
}
