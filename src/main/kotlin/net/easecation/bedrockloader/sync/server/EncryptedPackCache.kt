package net.easecation.bedrockloader.sync.server

import net.easecation.bedrockloader.sync.common.MD5Util
import net.easecation.bedrockloader.sync.common.PackEncryption
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 加密包缓存管理器
 *
 * 缓存加密后的资源包数据，避免每次 HTTP 请求都重新加密。
 * 通过原始文件的 MD5 判断缓存是否过期（文件内容变化时自动重新加密）。
 */
class EncryptedPackCache(
    private val packDirectory: File,
    private val keyManager: PackKeyManager
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/EncryptedPackCache")
    private val cache = mutableMapOf<String, CachedPack>()

    /**
     * 缓存的加密包数据
     */
    data class CachedPack(
        /** 加密后的数据 */
        val encryptedData: ByteArray,
        /** 加密数据的 MD5 */
        val md5: String,
        /** 加密数据的大小 */
        val size: Long,
        /** 原始文件的 MD5（用于检测文件变更） */
        val originalMd5: String,
        /** 缓存时间 */
        val timestamp: Long
    )

    /**
     * 获取加密后的包数据
     * 如果缓存有效则返回缓存，否则重新加密
     *
     * @param filename 包文件名
     * @param originalFile 原始（未加密的）包文件
     * @return 缓存的加密包
     */
    fun getEncryptedPack(filename: String, originalFile: File): CachedPack {
        val originalMd5 = MD5Util.calculateMD5(originalFile)

        // 检查缓存是否有效
        val cached = cache[filename]
        if (cached != null && cached.originalMd5 == originalMd5) {
            return cached
        }

        // 缓存无效，重新加密
        logger.info("Encrypting pack: $filename (${formatBytes(originalFile.length())})")

        val key = keyManager.getOrCreateKey(filename)
        val originalData = originalFile.readBytes()
        val encryptedData = PackEncryption.encrypt(originalData, key)
        val encryptedMd5 = PackEncryption.calculateMD5(encryptedData)

        val cachedPack = CachedPack(
            encryptedData = encryptedData,
            md5 = encryptedMd5,
            size = encryptedData.size.toLong(),
            originalMd5 = originalMd5,
            timestamp = System.currentTimeMillis()
        )

        cache[filename] = cachedPack
        logger.info("Pack encrypted: $filename (${formatBytes(originalFile.length())} -> ${formatBytes(cachedPack.size)})")

        return cachedPack
    }

    /**
     * 使指定包的缓存失效
     */
    fun invalidate(filename: String) {
        cache.remove(filename)
        logger.debug("Cache invalidated: $filename")
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        cache.clear()
        logger.info("All encrypted pack cache cleared")
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
