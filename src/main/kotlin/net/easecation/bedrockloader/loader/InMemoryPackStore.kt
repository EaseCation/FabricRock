package net.easecation.bedrockloader.loader

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存包存储注册表
 *
 * 存储客户端从服务端下载并在内存中解密的资源包数据 (InMemoryZipPack)。
 * 这些包的明文永远不会写入磁盘。
 *
 * 生命周期:
 * - PreLaunch 阶段: 下载密文 → 获取密钥 → 解密 → store()
 * - onInitialize 阶段: BedrockAddonsLoader.load() 从 getAll() 读取
 * - 游戏运行期间: 数据持续驻留内存
 */
object InMemoryPackStore {

    private val logger = LoggerFactory.getLogger("BedrockLoader/InMemoryPackStore")
    private val packs = ConcurrentHashMap<String, InMemoryZipPack>()

    /**
     * 存储一个解密后的包
     * @param filename 包文件名（如 "my_pack.zip"）
     * @param pack 解密后的内存 ZIP 包
     */
    fun store(filename: String, pack: InMemoryZipPack) {
        packs[filename] = pack
        logger.debug("Stored in-memory pack: {} ({} entries, {})",
            filename, pack.size(), formatBytes(pack.getMemorySize()))
    }

    /**
     * 获取一个包
     */
    fun get(filename: String): InMemoryZipPack? = packs[filename]

    /**
     * 获取所有包
     */
    fun getAll(): Map<String, InMemoryZipPack> = packs.toMap()

    /**
     * 检查是否有包
     */
    fun isEmpty(): Boolean = packs.isEmpty()

    /**
     * 包数量
     */
    fun size(): Int = packs.size

    /**
     * 清空所有内存包
     */
    fun clear() {
        val count = packs.size
        packs.clear()
        if (count > 0) {
            logger.info("Cleared {} in-memory pack(s)", count)
        }
    }

    /**
     * 获取总内存占用
     */
    fun getTotalMemorySize(): Long {
        return packs.values.sumOf { it.getMemorySize() }
    }

    /**
     * 打印内存使用信息
     */
    fun logMemoryUsage() {
        if (packs.isEmpty()) return
        logger.info("In-memory packs: {} pack(s), total size: {}",
            packs.size, formatBytes(getTotalMemorySize()))
        for ((name, pack) in packs) {
            logger.info("  - {} ({} entries, {})",
                name, pack.size(), formatBytes(pack.getMemorySize()))
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
