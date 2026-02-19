package net.easecation.bedrockloader.loader

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 内存中的 ZIP 包读取器
 *
 * 替代 java.util.zip.ZipFile（ZipFile 只能从磁盘文件读取）。
 * 用于从加密资源包解密后的字节数据中读取 ZIP 内容，避免将明文写入磁盘。
 *
 * 构造时使用 ZipInputStream 一次性读取所有 entry 到内存 Map 中，
 * 后续通过 Map 进行 O(1) 随机访问。
 */
class InMemoryZipPack(zipData: ByteArray) {

    /**
     * entry名称 -> 字节数据
     */
    private val entries: Map<String, ByteArray>

    /**
     * 所有 entry 名称集合
     */
    val entryNames: Set<String>
        get() = entries.keys

    init {
        val map = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipData)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    map[entry.name] = zis.readAllBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        entries = map
    }

    /**
     * 获取指定 entry 的数据
     * @param name entry 的完整路径名
     * @return 字节数据，不存在则返回 null
     */
    fun getEntry(name: String): ByteArray? = entries[name]

    /**
     * 获取指定 entry 的输入流
     * @param name entry 的完整路径名
     * @return 输入流，不存在则返回 null
     */
    fun getInputStream(name: String): InputStream? {
        return entries[name]?.let { ByteArrayInputStream(it) }
    }

    /**
     * 检查 entry 是否存在
     */
    fun hasEntry(name: String): Boolean = entries.containsKey(name)

    /**
     * 获取所有匹配前缀的 entry 名称
     * @param prefix 路径前缀（如 "textures/"、"behavior_pack/"）
     */
    fun getEntriesWithPrefix(prefix: String): List<String> {
        return entries.keys.filter { it.startsWith(prefix) }
    }

    /**
     * 获取 entry 数量
     */
    fun size(): Int = entries.size

    /**
     * 获取内存占用大小（近似值，仅计算 entry 数据）
     */
    fun getMemorySize(): Long {
        return entries.values.sumOf { it.size.toLong() }
    }
}
