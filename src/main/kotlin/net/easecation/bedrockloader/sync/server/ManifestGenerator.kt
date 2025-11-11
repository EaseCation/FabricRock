package net.easecation.bedrockloader.sync.server

import net.easecation.bedrockloader.loader.BedrockPackRegistry
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Resource Pack Manifest Generator
 *
 * Reads loaded pack info from BedrockPackRegistry and generates RemotePackManifest
 * Ensures HTTP server exposes the same packs as loaded by the game
 */
class ManifestGenerator(
    private val packDirectory: File,
    private val config: ServerConfig
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ManifestGenerator")

    /**
     * Generate resource pack manifest
     *
     * Reads loaded pack info from BedrockPackRegistry to ensure consistency
     *
     * @return RemotePackManifest object
     */
    fun generate(): RemotePackManifest {
        // Get all loaded packs from registry
        val allPacks = BedrockPackRegistry.getAllPacks()

        if (allPacks.isEmpty()) {
            logger.warn("No resource packs found")
        }

        // Convert to RemotePackInfo
        val packInfoList = allPacks.map { packInfo ->
            RemotePackInfo(
                name = packInfo.file.name,
                uuid = packInfo.id,           // Get UUID from registry
                version = packInfo.version,   // Get version from registry
                md5 = packInfo.md5,
                size = packInfo.size,
                url = generateDownloadUrl(packInfo.file.name)
            )
        }

        // Create manifest object
        val manifest = RemotePackManifest(
            version = "1.0",
            generatedAt = System.currentTimeMillis(),
            serverVersion = getServerVersion(),
            packs = packInfoList
        )

        logger.info("Manifest generated: ${manifest.getPackCount()} pack(s), total size ${formatFileSize(manifest.getTotalSize())}")

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
