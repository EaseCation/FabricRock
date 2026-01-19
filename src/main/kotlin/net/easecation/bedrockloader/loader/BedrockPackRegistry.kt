package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.pack.PackManifest
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 基岩版资源包统一注册表
 *
 * 所有已加载的资源包都会在此注册，包括：
 * - 包的元数据（ID、名称、版本等）
 * - 文件路径（ZIP/MCPACK文件或缓存的ZIP）
 * - MD5哈希和文件大小
 *
 * 用于：
 * - HTTP服务器生成manifest.json
 * - 确保游戏加载和远程同步的包列表一致
 */
object BedrockPackRegistry {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PackRegistry")

    /**
     * 包信息数据类
     */
    data class PackInfo(
        val id: String,              // 包的UUID
        val name: String,            // 包名称
        val version: String,         // 版本号（例如：1.0.0）
        val type: String,            // 包类型："resources" 或 "data"
        val file: File,              // 文件路径（ZIP/MCPACK/MCADDON或缓存的文件）
        val md5: String,             // MD5哈希值
        val size: Long,              // 文件大小（字节）
        val manifest: PackManifest,  // 完整的manifest对象
        val addonName: String? = null,  // 所属Addon名称（如果来自addon）
        val isFromAddon: Boolean = false // 是否来自addon目录或.mcaddon文件
    )

    /**
     * 包注册表（线程安全）
     * key: 包ID（UUID）
     * value: 包信息
     */
    private val packages = ConcurrentHashMap<String, PackInfo>()

    /**
     * 注册一个资源包
     *
     * @param info 包信息
     */
    fun register(info: PackInfo) {
        packages[info.id] = info
        logger.info("注册资源包: ${info.name} [${info.id}] - ${info.type} - ${formatBytes(info.size)}")
    }

    /**
     * 获取所有已注册的包
     *
     * @return 包信息列表
     */
    fun getAllPacks(): List<PackInfo> {
        return packages.values.toList()
    }

    /**
     * 根据ID获取包信息
     *
     * @param id 包ID
     * @return 包信息，如果不存在则返回null
     */
    fun getPackById(id: String): PackInfo? {
        return packages[id]
    }

    /**
     * 获取指定类型的所有包
     *
     * @param type 包类型（"resources" 或 "data"）
     * @return 包信息列表
     */
    fun getPacksByType(type: String): List<PackInfo> {
        return packages.values.filter { it.type == type }
    }

    /**
     * 获取所有独立的资源文件（用于远程同步）
     * 对于addon中的包，返回的是.mcaddon文件；对于独立包，返回的是.zip文件
     *
     * 对于addon类型的包（同一个.mcaddon文件包含多个子包），会生成组合UUID，
     * 确保与客户端LocalPackScanner生成的UUID一致。
     *
     * @return 资源文件映射（文件名 -> 文件信息）
     */
    fun getDistinctResourceFiles(): Map<String, PackInfo> {
        val result = mutableMapOf<String, PackInfo>()
        // 按文件名分组，收集同一个文件的所有子包UUID
        val fileToUUIDs = mutableMapOf<String, MutableList<String>>()
        val fileToFirstPack = mutableMapOf<String, PackInfo>()

        for (pack in packages.values) {
            val fileName = pack.file.name
            fileToUUIDs.getOrPut(fileName) { mutableListOf() }.add(pack.id)
            if (!fileToFirstPack.containsKey(fileName)) {
                fileToFirstPack[fileName] = pack
            }
        }

        // 为每个文件生成PackInfo
        for ((fileName, uuids) in fileToUUIDs) {
            val firstPack = fileToFirstPack[fileName] ?: continue

            if (uuids.size > 1) {
                // addon类型：生成组合UUID
                uuids.sort()
                val combinedString = uuids.joinToString("|")
                val combinedUUID = java.util.UUID.nameUUIDFromBytes(combinedString.toByteArray()).toString()

                logger.debug("addon $fileName 组合UUID: $combinedUUID (来自 ${uuids.size} 个子包: ${uuids.joinToString(", ")})")

                // 创建新的PackInfo，使用组合UUID
                result[fileName] = firstPack.copy(id = combinedUUID)
            } else {
                // 单包：直接使用原始UUID
                result[fileName] = firstPack
            }
        }

        return result
    }

    /**
     * 获取指定addon下的所有包
     *
     * @param addonName addon名称
     * @return 包信息列表
     */
    fun getPacksByAddon(addonName: String): List<PackInfo> {
        return packages.values.filter { it.addonName == addonName }
    }

    /**
     * 获取所有addon名称
     *
     * @return addon名称列表
     */
    fun getAllAddonNames(): List<String> {
        return packages.values
            .mapNotNull { it.addonName }
            .distinct()
    }

    /**
     * 清空注册表
     * 主要用于测试
     */
    fun clear() {
        logger.debug("清空包注册表")
        packages.clear()
    }

    /**
     * 获取注册包的数量
     */
    fun getPackCount(): Int {
        return packages.size
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
