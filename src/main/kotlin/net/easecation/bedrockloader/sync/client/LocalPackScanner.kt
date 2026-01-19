package net.easecation.bedrockloader.sync.client

import com.google.gson.reflect.TypeToken
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.sync.common.MD5Util
import net.easecation.bedrockloader.sync.common.ResourceType
import net.easecation.bedrockloader.util.GsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

/**
 * 本地资源包扫描器
 * 扫描本地的资源包文件（包括手动放置的包和remote/目录中远程下载的包）
 * 提取包的UUID、版本信息并计算MD5，用于同步比较和冲突检测
 *
 * 支持两种资源类型：
 * - PACK: 单个包（.zip/.mcpack）
 * - ADDON: 包含多个子包的addon（.mcaddon）
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
        val uuid: String?,      // 包的UUID（从manifest.json的header.uuid提取，对于addon是第一个子包的UUID）
        val version: String?,   // 包的版本（从manifest.json的header.version提取）
        val md5: String,
        val size: Long,
        val file: File,
        val isManual: Boolean,  // 是否为手动放置的包（非remote/目录）
        val type: ResourceType = ResourceType.PACK  // 资源类型：PACK 或 ADDON
    ) {
        /**
         * 是否为addon
         */
        fun isAddon(): Boolean = type == ResourceType.ADDON
    }

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
        val resourceType = detectResourceType(file)
        logger.debug("扫描文件: ${file.name} (${if (isManual) "手动" else "远程"}, ${resourceType})")

        // 提取UUID和版本信息
        val (uuid, version) = extractPackMetadata(file)

        val md5 = MD5Util.calculateMD5(file)
        val size = file.length()

        logger.debug("  - 类型: $resourceType")
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
            isManual = isManual,
            type = resourceType
        )
    }

    /**
     * 检测资源类型
     */
    private fun detectResourceType(file: File): ResourceType {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".mcaddon") -> ResourceType.ADDON
            else -> ResourceType.PACK
        }
    }

    /**
     * 从资源包ZIP文件中提取UUID和版本信息
     * 支持单包（.zip/.mcpack）和addon（.mcaddon）
     *
     * 对于addon（.mcaddon），会收集所有子包的UUID并生成组合UUID，
     * 确保无论子包顺序如何，生成的UUID都是确定性的。
     *
     * @param file 资源包文件
     * @return Pair(UUID, Version)，如果提取失败则返回(null, null)
     */
    private fun extractPackMetadata(file: File): Pair<String?, String?> {
        try {
            ZipFile(file).use { zip ->
                // 首先尝试在根目录查找manifest.json（单包）
                val rootEntry = zip.getEntry("manifest.json") ?: zip.getEntry("pack_manifest.json")
                if (rootEntry != null) {
                    return parseManifest(zip, rootEntry, file.name)
                }

                // 如果根目录没有，查找子目录中的所有manifest.json（.mcaddon）
                val subManifestEntries = zip.entries().asSequence()
                    .filter { entry ->
                        val name = entry.name
                        (name.endsWith("manifest.json") || name.endsWith("pack_manifest.json")) &&
                        name.count { it == '/' } == 1 // 只在第一层子目录中
                    }
                    .toList()

                if (subManifestEntries.isEmpty()) {
                    logger.warn("文件 ${file.name} 中找不到manifest.json")
                    return Pair(null, null)
                }

                // 对于addon，收集所有子包的UUID并生成组合UUID
                if (subManifestEntries.size > 1) {
                    return extractAddonCombinedMetadata(zip, subManifestEntries, file.name)
                }

                // 只有一个子包，直接使用其UUID
                return parseManifest(zip, subManifestEntries.first(), file.name)
            }
        } catch (e: Exception) {
            logger.warn("提取 ${file.name} 的metadata失败: ${e.message}")
            return Pair(null, null)
        }
    }

    /**
     * 为addon提取组合UUID
     * 收集所有子包的UUID，排序后生成确定性的组合UUID
     *
     * @param zip ZIP文件
     * @param entries 所有子包的manifest entries
     * @param fileName 文件名（用于日志）
     * @return Pair(组合UUID, 第一个子包的版本)
     */
    private fun extractAddonCombinedMetadata(
        zip: ZipFile,
        entries: List<java.util.zip.ZipEntry>,
        fileName: String
    ): Pair<String?, String?> {
        val uuids = mutableListOf<String>()
        var firstVersion: String? = null

        for (entry in entries) {
            val (uuid, version) = parseManifest(zip, entry, fileName)
            if (uuid != null) {
                uuids.add(uuid)
                if (firstVersion == null) {
                    firstVersion = version
                }
            }
        }

        if (uuids.isEmpty()) {
            logger.warn("addon $fileName 中没有找到有效的UUID")
            return Pair(null, null)
        }

        // 排序UUID列表，确保顺序一致
        uuids.sort()

        // 生成组合UUID（使用UUID v5基于名称空间）
        val combinedString = uuids.joinToString("|")
        val combinedUUID = java.util.UUID.nameUUIDFromBytes(combinedString.toByteArray()).toString()

        logger.debug("addon $fileName 组合UUID: $combinedUUID (来自 ${uuids.size} 个子包: ${uuids.joinToString(", ")})")

        return Pair(combinedUUID, firstVersion)
    }

    /**
     * 解析manifest.json
     */
    private fun parseManifest(zip: ZipFile, entry: java.util.zip.ZipEntry, fileName: String): Pair<String?, String?> {
        return try {
            val type = object : TypeToken<PackManifest>() {}.type
            val manifest: PackManifest = GsonUtil.GSON.fromJson(
                InputStreamReader(zip.getInputStream(entry)),
                type
            )

            val uuid = manifest.header?.uuid?.toString()
            val version = manifest.header?.version?.toString()

            Pair(uuid, version)
        } catch (e: Exception) {
            logger.warn("解析 $fileName 中的 ${entry.name} 失败: ${e.message}")
            Pair(null, null)
        }
    }

    /**
     * 判断文件是否为有效的资源包文件
     * 支持 .zip, .mcpack, .mcaddon
     */
    private fun isValidPackFile(file: File, name: String): Boolean {
        return file.isFile
                && (name.endsWith(".zip") || name.endsWith(".mcpack") || name.endsWith(".mcaddon"))
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
