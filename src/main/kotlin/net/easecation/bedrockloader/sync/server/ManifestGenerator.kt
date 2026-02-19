package net.easecation.bedrockloader.sync.server

import net.easecation.bedrockloader.loader.BedrockPackRegistry
import net.easecation.bedrockloader.sync.common.EncryptionConfig
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import net.easecation.bedrockloader.sync.common.ResourceType
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Resource Pack Manifest Generator
 *
 * Reads loaded pack info from BedrockPackRegistry and generates RemotePackManifest
 * Ensures HTTP server exposes the same packs as loaded by the game
 *
 * 支持两种资源类型：
 * - PACK: 单个包（.zip/.mcpack）
 * - ADDON: 包含多个子包的addon（.mcaddon）
 */
class ManifestGenerator(
    private val packDirectory: File,
    private val config: ServerConfig,
    private val encryptedPackCache: EncryptedPackCache? = null,
    private val serverToken: String? = null
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ManifestGenerator")

    /**
     * Generate resource pack manifest
     *
     * Reads loaded pack info from BedrockPackRegistry to ensure consistency
     * 使用 getDistinctResourceFiles() 确保每个资源文件只出现一次
     * （addon中的多个包共享同一个.mcaddon文件）
     *
     * @return RemotePackManifest object
     */
    fun generate(): RemotePackManifest {
        // 获取不重复的资源文件（同一个addon的多个包只返回一个文件）
        val distinctFiles = BedrockPackRegistry.getDistinctResourceFiles()

        if (distinctFiles.isEmpty()) {
            logger.warn("No resource packs found")
        }

        val isEncrypted = config.encryptionEnabled && encryptedPackCache != null

        // Convert to RemotePackInfo
        val packInfoList = distinctFiles.map { (fileName, packInfo) ->
            // 判断资源类型
            val resourceType = when {
                fileName.endsWith(".mcaddon", ignoreCase = true) -> ResourceType.ADDON
                packInfo.isFromAddon -> ResourceType.ADDON
                else -> ResourceType.PACK
            }

            // 加密模式下使用密文的 MD5/size
            val (md5, size) = if (isEncrypted) {
                val packDir = File(packDirectory, ".cache")
                var file = File(packDir, fileName)
                if (!file.exists()) file = File(packDirectory, fileName)
                if (file.exists()) {
                    val cached = encryptedPackCache!!.getEncryptedPack(fileName, file)
                    Pair(cached.md5, cached.size)
                } else {
                    Pair(packInfo.md5, packInfo.size)
                }
            } else {
                Pair(packInfo.md5, packInfo.size)
            }

            RemotePackInfo(
                name = fileName,
                type = resourceType,
                uuid = packInfo.id,
                version = packInfo.version,
                md5 = md5,
                size = size,
                url = generateDownloadUrl(fileName),
                encrypted = isEncrypted
            )
        }

        // 构建加密配置（仅在启用时）
        val encryptionConfig = if (isEncrypted && serverToken != null) {
            EncryptionConfig(
                enabled = true,
                serverToken = serverToken
            )
        } else {
            null
        }

        // Create manifest object
        val manifest = RemotePackManifest(
            version = RemotePackManifest.CURRENT_VERSION,
            generatedAt = System.currentTimeMillis(),
            serverVersion = getServerVersion(),
            packs = packInfoList,
            encryption = encryptionConfig
        )

        // 打印统计信息
        val addonCount = manifest.getAddons().size
        val packCount = manifest.getSinglePacks().size
        val encryptedStr = if (isEncrypted) " [ENCRYPTED]" else ""
        logger.info("Manifest generated: $addonCount addon(s), $packCount pack(s), total size ${formatFileSize(manifest.getTotalSize())}$encryptedStr")

        return manifest
    }

    /**
     * Generate download URL
     *
     * @param filename File name
     * @return Download URL (relative path)
     */
    private fun generateDownloadUrl(filename: String): String {
        // Use relative path, client will concatenate full URL based on server address
        return "/packs/$filename"
    }

    /**
     * Get server version info
     */
    private fun getServerVersion(): String {
        return "BedrockLoader-1.0.0" // Can be read from mod version
    }

    /**
     * Format file size to human-readable string
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / 1024.0 / 1024.0)
            else -> String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0)
        }
    }
}
