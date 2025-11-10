package net.easecation.bedrockloader.sync.server

import net.easecation.bedrockloader.loader.BedrockPackRegistry
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 资源包清单生成器
 *
 * 从BedrockPackRegistry读取已加载的包信息，生成RemotePackManifest对象
 * 确保HTTP服务器暴露的包与游戏加载的包100%一致
 */
class ManifestGenerator(
    private val packDirectory: File,
    private val config: ServerConfig
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/ManifestGenerator")

    /**
     * 生成资源包清单
     *
     * 从BedrockPackRegistry读取已加载的包信息，确保与游戏加载的包一致
     *
     * @return RemotePackManifest对象
     */
    fun generate(): RemotePackManifest {
        logger.info("开始生成资源包清单...")

        // 直接从包注册表获取所有已加载的包
        val allPacks = BedrockPackRegistry.getAllPacks()

        if (allPacks.isEmpty()) {
            logger.warn("未找到任何资源包")
        } else {
            logger.debug("从注册表读取到 ${allPacks.size} 个资源包")
        }

        // 转换为RemotePackInfo
        val packInfoList = allPacks.map { packInfo ->
            RemotePackInfo(
                name = packInfo.file.name,
                uuid = packInfo.id,           // 从注册表获取UUID
                version = packInfo.version,   // 从注册表获取版本
                md5 = packInfo.md5,
                size = packInfo.size,
                url = generateDownloadUrl(packInfo.file.name)
            )
        }

        // 创建清单对象
        val manifest = RemotePackManifest(
            version = "1.0",
            generatedAt = System.currentTimeMillis(),
            serverVersion = getServerVersion(),
            packs = packInfoList
        )

        logger.info("清单生成完成: ${manifest.getPackCount()} 个资源包, 总大小 ${formatFileSize(manifest.getTotalSize())}")

        return manifest
    }

    /**
     * 生成下载URL
     *
     * @param filename 文件名
     * @return 下载URL（相对路径）
     */
    private fun generateDownloadUrl(filename: String): String {
        // 使用相对路径，客户端会根据服务器地址拼接完整URL
        return "/packs/$filename"
    }

    /**
     * 获取服务器版本信息
     */
    private fun getServerVersion(): String {
        return "BedrockLoader-1.0.0" // 可以从模组版本读取
    }

    /**
     * 格式化文件大小为人类可读的字符串
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
