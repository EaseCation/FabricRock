package net.easecation.bedrockloader.loader

import com.google.common.io.Files
import com.google.gson.Gson
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.pack.BedrockPack
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.bedrock.pack.ZippedBedrockPack
import net.easecation.bedrockloader.loader.deserializer.BedrockBehaviorDeserializer
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.loader.deserializer.BedrockResourceDeserializer
import net.easecation.bedrockloader.sync.common.MD5Util
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * BedrockAddonLoader is the main class for loading bedrock packs.
 * It will load all the bedrock packs from the data folder and load them into the game.
 *
 * 加载流程:
 * 基岩版行为包/材质包 -> 解码器Deserializer(将来要做自动版本提升) -> 混合到同一个BedrockPackContext ->
 * 材质包加载器(转换为临时java版材质包、准备资源Provider) -> 行为包加载器（注册方块和物品到java服务端）
 */
object BedrockAddonsLoader {

    private val resourcePackMap: MutableMap<String, BedrockPack> = HashMap<String, BedrockPack>()
    private val behaviourPackMap: MutableMap<String, BedrockPack> = HashMap<String, BedrockPack>()

    val context = BedrockPackContext()

    fun load() {
        val dataFolder: File = BedrockLoader.getGameDir().resolve("config/bedrock-loader")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // 创建缓存目录（用于存储文件夹包的ZIP）
        val cacheDir = File(dataFolder, ".cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // 创建remote目录（用于存储远程下载的包）
        val remoteDir = File(dataFolder, "remote")
        if (!remoteDir.exists()) {
            remoteDir.mkdirs()
        }

        // 从dataFolder根目录读取手动放置的包（排除remote和.cache目录）
        val rootFiles = dataFolder.listFiles { file: File, name: String ->
            val child = File(file, name)
            (child.isDirectory && name != ".DS_Store" && name != "remote" && name != ".cache") ||
            name.endsWith(".zip") || name.endsWith(".mcpack")
        } ?: emptyArray()

        // 从remote/目录读取远程下载的包
        val remoteFiles = remoteDir.listFiles { file: File, name: String ->
            val child = File(file, name)
            (child.isDirectory && name != ".DS_Store") || name.endsWith(".zip") || name.endsWith(".mcpack")
        } ?: emptyArray()

        // 合并两个目录的文件
        // 重要：先加载远程包，再加载手动包，确保手动包优先（覆盖远程包）
        val files = remoteFiles + rootFiles

        if (files.isEmpty()) {
            BedrockLoader.logger.warn("No bedrock pack found in " + dataFolder.absolutePath)
            return
        }

        // 打印详细的包列表，用于调试
        BedrockLoader.logger.info("========== 识别到的资源包列表 ==========")
        BedrockLoader.logger.info("手动放置的包 (${rootFiles.size}):")
        for (file in rootFiles) {
            val type = if (file.isDirectory) "目录" else "文件"
            BedrockLoader.logger.info("  - $type: ${file.absolutePath}")
        }
        BedrockLoader.logger.info("远程下载的包 (${remoteFiles.size}):")
        for (file in remoteFiles) {
            val type = if (file.isDirectory) "目录" else "文件"
            BedrockLoader.logger.info("  - $type: ${file.absolutePath}")
        }
        BedrockLoader.logger.info("========================================")

        BedrockLoader.logger.info("找到 ${rootFiles.size} 个手动放置的包, ${remoteFiles.size} 个远程下载的包")
        BedrockLoader.logger.info("加载顺序: 先远程包，后手动包（手动包优先覆盖）")

        // 读取zip文件
        for (file in files) {
            try {
                var f: File = file
                var isDirectoryPack = false

                if (file.isDirectory) {
                    loadDirectoryPack(file, cacheDir)?.let {
                        f = it
                        isDirectoryPack = true
                    } ?: continue
                }

                val pack: BedrockPack = ZippedBedrockPack(f)
                val packId = pack.getPackId() ?: continue
                val packType = pack.getPackType()

                if (packType.equals("resources")) {
                    // 只添加到材质包管理器中
                    resourcePackMap[packId] = pack
                    context.resource.putAll(BedrockResourceDeserializer.deserialize(ZipFile(f)))
                    BedrockLoader.logger.info((("Reading resource pack: " + pack.getPackName()) + "[" + packId) + "]")
                } else if (packType.equals("data")) {
                    // 行为包，需要读取内容
                    behaviourPackMap[packId] = pack
                    context.behavior.putAll(BedrockBehaviorDeserializer.deserialize(ZipFile(f)))
                    BedrockLoader.logger.info((("Reading behaviour pack: " + pack.getPackName()) + "[" + packId) + "]")
                }

                // 注册包到统一注册表
                registerPack(pack, f, isDirectoryPack)

            } catch (e: Exception) {
                BedrockLoader.logger.warn("Failed to load pack " + file.name, e)
            }
        }
    }

    /**
     * 计算文件夹内容的哈希值
     * 基于所有文件的相对路径和内容计算MD5，确保内容一致性
     *
     * @param directory 文件夹路径
     * @return 内容哈希字符串
     */
    private fun calculateDirectoryContentHash(directory: File): String {
        val md = java.security.MessageDigest.getInstance("MD5")

        // 获取所有文件并排序（确保顺序一致）
        val files = TreeSet<File>(FileUtils.listFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE))

        for (file in files) {
            // 添加相对路径到哈希
            val relativePath = directory.toPath().relativize(file.toPath()).toString()
            md.update(relativePath.toByteArray(Charsets.UTF_8))

            // 添加文件内容到哈希
            md.update(Files.toByteArray(file))
        }

        // 转换为16进制字符串
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 加载文件夹形式的资源包
     * 将文件夹打包为ZIP并缓存，避免每次启动都重新打包
     * 使用内容哈希判断缓存是否有效，确保ZIP文件的MD5稳定性
     *
     * @param directory 文件夹路径
     * @param cacheDir 缓存目录
     * @return 打包后的ZIP文件，如果失败则返回null
     */
    private fun loadDirectoryPack(directory: File, cacheDir: File): File? {
        val manifestFile = File(directory, "manifest.json")
        if (!manifestFile.exists() || !manifestFile.isFile) {
            return null
        }

        // 缓存文件路径
        val cacheFile = File(cacheDir, "${directory.name}.zip")
        val hashFile = File(cacheDir, "${directory.name}.hash")

        // 计算当前文件夹的内容哈希
        val currentHash = try {
            calculateDirectoryContentHash(directory)
        } catch (e: Exception) {
            BedrockLoader.logger.error("计算文件夹哈希失败: ${directory.name}", e)
            return null
        }

        // 检查缓存是否有效（比对内容哈希）
        if (cacheFile.exists() && hashFile.exists()) {
            val cachedHash = try {
                hashFile.readText().trim()
            } catch (e: Exception) {
                ""
            }

            if (cachedHash == currentHash) {
                BedrockLoader.logger.debug("使用缓存的文件夹包: ${directory.name} (哈希: ${currentHash.substring(0, 8)}...)")
                return cacheFile
            } else {
                BedrockLoader.logger.debug("文件夹内容已改变，需要重新打包: ${directory.name}")
                BedrockLoader.logger.debug("  旧哈希: ${cachedHash.substring(0, 8)}...")
                BedrockLoader.logger.debug("  新哈希: ${currentHash.substring(0, 8)}...")
            }
        }

        // 重新打包
        BedrockLoader.logger.info("打包文件夹资源包: ${directory.name}")

        try {
            val time = FileTime.fromMillis(0)
            ZipOutputStream(FileOutputStream(cacheFile)).use { stream ->
                stream.setLevel(Deflater.BEST_COMPRESSION)
                val files: Collection<File> = TreeSet(FileUtils.listFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE))
                for (file in files) {
                    val entry = ZipEntry(directory.toPath().relativize(file.toPath()).toString().replace("\\", "/"))
                            .setCreationTime(time)
                            .setLastModifiedTime(time)
                            .setLastAccessTime(time)
                    stream.putNextEntry(entry)
                    stream.write(Files.toByteArray(file))
                    stream.closeEntry()
                }
            }
            BedrockLoader.logger.info("文件夹包打包完成: ${directory.name} -> ${cacheFile.name}")

            // 保存内容哈希到文件，用于下次启动时判断缓存是否有效
            try {
                hashFile.writeText(currentHash)
                BedrockLoader.logger.debug("保存内容哈希: ${currentHash.substring(0, 8)}... -> ${hashFile.name}")
            } catch (e: Exception) {
                BedrockLoader.logger.warn("保存哈希文件失败: ${hashFile.name}", e)
            }

        } catch (e: IOException) {
            BedrockLoader.logger.error("无法打包文件夹资源包: ${directory.name}", e)
            return null
        }

        return cacheFile
    }

    /**
     * 注册资源包到统一注册表
     *
     * @param pack 基岩包对象
     * @param file ZIP文件
     * @param isDirectoryPack 是否是文件夹包
     */
    private fun registerPack(pack: BedrockPack, file: File, isDirectoryPack: Boolean) {
        try {
            // 从BedrockPack获取包信息（已经解析好的）
            val packId = pack.getPackId() ?: return
            val packName = pack.getPackName() ?: "未知包"
            val packVersion = pack.getPackVersion() ?: "0.0.0"
            val packType = if (pack.getPackType().equals("resources")) "resources" else "data"

            // 计算MD5和文件大小
            val md5 = MD5Util.calculateMD5(file)
            val size = file.length()

            // 创建一个简化的manifest对象（仅用于存储基本信息）
            val manifest = PackManifest().apply {
                header = PackManifest.Header().apply {
                    name = packName
                    uuid = java.util.UUID.fromString(packId)
                    // version使用字符串表示，避免解析问题
                }
            }

            // 创建包信息
            val packInfo = BedrockPackRegistry.PackInfo(
                id = packId,
                name = packName,
                version = packVersion,
                type = packType,
                file = file,
                md5 = md5,
                size = size,
                manifest = manifest
            )

            // 注册到统一注册表
            BedrockPackRegistry.register(packInfo)

        } catch (e: Exception) {
            BedrockLoader.logger.error("注册包失败: ${file.name}", e)
        }
    }

}