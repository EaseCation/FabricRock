package net.easecation.bedrockloader.loader

import com.google.common.io.Files
import com.google.gson.reflect.TypeToken
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.util.GsonUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Addon扫描器
 * 用于识别和处理addon目录结构、.mcaddon文件
 */
object AddonScanner {

    /**
     * .mcaddon中的子包信息
     */
    data class McAddonEntry(
        val packPath: String,           // 包在mcaddon内的路径前缀（如 "behavior_pack/"）
        val manifest: PackManifest,     // 包的manifest
        val mcaddonFile: File           // mcaddon文件
    )

    /**
     * 检测目录/文件的结构类型
     */
    fun detectStructureType(file: File): PackStructureType {
        if (!file.exists()) {
            return PackStructureType.UNKNOWN
        }

        if (file.isFile) {
            return when {
                file.name.endsWith(".mcaddon", ignoreCase = true) -> PackStructureType.MCADDON_FILE
                file.name.endsWith(".mcpack", ignoreCase = true) -> PackStructureType.MCPACK_FILE
                file.name.endsWith(".zip", ignoreCase = true) -> {
                    // 检查ZIP内是否有manifest.json，以区分mcaddon和mcpack
                    if (hasManifestInZipRoot(file)) {
                        PackStructureType.MCPACK_FILE
                    } else if (hasSubPacksInZip(file)) {
                        // ZIP内有子目录包含manifest.json，视为mcaddon
                        PackStructureType.MCADDON_FILE
                    } else {
                        PackStructureType.UNKNOWN
                    }
                }
                else -> PackStructureType.UNKNOWN
            }
        }

        if (file.isDirectory) {
            // 检查当前目录是否有manifest.json
            if (File(file, "manifest.json").exists() || File(file, "pack_manifest.json").exists()) {
                return PackStructureType.SINGLE_PACK
            }

            // 检查子目录是否有manifest.json（只检查一层）
            val subDirs = file.listFiles { f -> f.isDirectory } ?: emptyArray()
            val hasSubPacks = subDirs.any { subDir ->
                File(subDir, "manifest.json").exists() || File(subDir, "pack_manifest.json").exists()
            }

            return if (hasSubPacks) PackStructureType.ADDON_DIRECTORY else PackStructureType.UNKNOWN
        }

        return PackStructureType.UNKNOWN
    }

    /**
     * 检查ZIP根目录是否有manifest.json
     */
    private fun hasManifestInZipRoot(zipFile: File): Boolean {
        return try {
            ZipFile(zipFile).use { zip ->
                zip.getEntry("manifest.json") != null || zip.getEntry("pack_manifest.json") != null
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查ZIP是否包含子目录，且子目录有manifest.json
     */
    private fun hasSubPacksInZip(zipFile: File): Boolean {
        return try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().asSequence()
                    .filter { it.name.endsWith("manifest.json") || it.name.endsWith("pack_manifest.json") }
                    .filter { it.name.contains("/") } // 必须在子目录中
                    .toList()
                entries.isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 将addon目录打包为.mcaddon
     * Addon目录结构将被保留在ZIP内
     *
     * @param addonDir Addon目录
     * @param cacheDir 缓存目录
     * @return 缓存的.mcaddon文件
     */
    fun packageAddonDirectory(addonDir: File, cacheDir: File): File? {
        val cacheFile = File(cacheDir, "${addonDir.name}.mcaddon")
        val hashFile = File(cacheDir, "${addonDir.name}.mcaddon.hash")

        // 计算当前目录的内容哈希
        val currentHash = try {
            calculateDirectoryContentHash(addonDir)
        } catch (e: Exception) {
            BedrockLoader.logger.error("计算Addon目录哈希失败: ${addonDir.name}", e)
            return null
        }

        // 检查缓存是否有效
        if (cacheFile.exists() && hashFile.exists()) {
            val cachedHash = try {
                hashFile.readText().trim()
            } catch (e: Exception) {
                ""
            }

            if (cachedHash == currentHash) {
                BedrockLoader.logger.debug("使用缓存的Addon包: ${addonDir.name} (哈希: ${currentHash.take(8)}...)")
                return cacheFile
            } else {
                BedrockLoader.logger.debug("Addon目录内容已改变，需要重新打包: ${addonDir.name}")
            }
        }

        // 重新打包
        BedrockLoader.logger.info("打包Addon目录: ${addonDir.name}")

        try {
            val time = FileTime.fromMillis(0)
            ZipOutputStream(FileOutputStream(cacheFile)).use { stream ->
                stream.setLevel(Deflater.BEST_COMPRESSION)

                // 获取所有子包目录
                val subDirs = addonDir.listFiles { f ->
                    f.isDirectory && (File(f, "manifest.json").exists() || File(f, "pack_manifest.json").exists())
                } ?: emptyArray()

                for (subDir in subDirs) {
                    // 遍历子包目录下的所有文件
                    val files: Collection<File> = TreeSet(FileUtils.listFiles(subDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE))
                    for (file in files) {
                        // 路径格式: {subDirName}/path/to/file
                        val relativePath = "${subDir.name}/${subDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")}"
                        val entry = ZipEntry(relativePath)
                            .setCreationTime(time)
                            .setLastModifiedTime(time)
                            .setLastAccessTime(time)
                        stream.putNextEntry(entry)
                        stream.write(Files.toByteArray(file))
                        stream.closeEntry()
                    }
                }
            }

            BedrockLoader.logger.info("Addon包打包完成: ${addonDir.name} -> ${cacheFile.name}")

            // 保存内容哈希
            try {
                hashFile.writeText(currentHash, Charsets.UTF_8)
            } catch (e: Exception) {
                BedrockLoader.logger.warn("保存Addon哈希文件失败: ${hashFile.name}", e)
            }

        } catch (e: Exception) {
            BedrockLoader.logger.error("无法打包Addon目录: ${addonDir.name}", e)
            return null
        }

        return cacheFile
    }

    /**
     * 从.mcaddon文件中加载所有包的入口信息
     *
     * @param mcaddonFile .mcaddon文件
     * @return 每个子包的入口信息列表
     */
    fun loadMcAddon(mcaddonFile: File): List<McAddonEntry> {
        val result = mutableListOf<McAddonEntry>()

        try {
            ZipFile(mcaddonFile).use { zip ->
                // 找出所有manifest.json的位置
                val manifestEntries = zip.entries().asSequence()
                    .filter { entry ->
                        val name = entry.name
                        (name.endsWith("manifest.json") || name.endsWith("pack_manifest.json")) &&
                        name.count { it == '/' } == 1 // 只在第一层子目录中
                    }
                    .toList()

                for (manifestEntry in manifestEntries) {
                    try {
                        // 解析manifest
                        val type = object : TypeToken<PackManifest>() {}.type
                        val manifest: PackManifest = GsonUtil.GSON.fromJson(
                            InputStreamReader(zip.getInputStream(manifestEntry)),
                            type
                        )

                        if (manifest.isValid()) {
                            // 提取包路径前缀（如 "behavior_pack/"）
                            val packPath = manifestEntry.name.substringBeforeLast("/") + "/"

                            result.add(McAddonEntry(
                                packPath = packPath,
                                manifest = manifest,
                                mcaddonFile = mcaddonFile
                            ))

                            BedrockLoader.logger.debug("发现子包: ${manifest.header?.name} (${packPath})")
                        }
                    } catch (e: Exception) {
                        BedrockLoader.logger.warn("解析manifest失败: ${manifestEntry.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            BedrockLoader.logger.error("无法读取.mcaddon文件: ${mcaddonFile.name}", e)
        }

        return result
    }

    /**
     * 计算目录内容的哈希值
     * 只计算包含manifest.json的子目录
     */
    private fun calculateDirectoryContentHash(directory: File): String {
        val md = java.security.MessageDigest.getInstance("MD5")

        // 获取所有子包目录
        val subDirs = directory.listFiles { f ->
            f.isDirectory && (File(f, "manifest.json").exists() || File(f, "pack_manifest.json").exists())
        }?.sortedBy { it.name } ?: emptyList()

        for (subDir in subDirs) {
            // 获取子目录下所有文件并排序
            val files = TreeSet<File>(FileUtils.listFiles(subDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE))

            for (file in files) {
                // 添加相对路径到哈希
                val relativePath = "${subDir.name}/${subDir.toPath().relativize(file.toPath())}"
                md.update(relativePath.toByteArray(Charsets.UTF_8))

                // 添加文件内容到哈希
                md.update(Files.toByteArray(file))
            }
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
