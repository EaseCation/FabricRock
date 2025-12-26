package net.easecation.bedrockloader.loader

import com.google.common.io.Files
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.pack.BedrockPack
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.bedrock.pack.ZippedBedrockPack
import net.easecation.bedrockloader.loader.deserializer.BedrockBehaviorDeserializer
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.loader.deserializer.BedrockResourceDeserializer
import net.easecation.bedrockloader.sync.client.ClientConfigLoader
import net.easecation.bedrockloader.sync.common.MD5Util
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File
import java.io.FileOutputStream
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
 *
 * 支持的结构:
 * 1. 单个包目录（包含manifest.json）
 * 2. 单个包文件（.zip/.mcpack）
 * 3. Addon目录（包含多个子包目录，每个子包有自己的manifest.json）
 * 4. .mcaddon文件（包含多个子包的ZIP）
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

        // 创建缓存目录（用于存储文件夹包的ZIP和addon的mcaddon）
        val cacheDir = File(dataFolder, ".cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // 仅在客户端且启用远程同步时创建remote目录（用于存储远程下载的包）
        val envType = FabricLoader.getInstance().environmentType
        if (envType == EnvType.CLIENT) {
            val configFile = File(dataFolder, "client.yml")
            val config = ClientConfigLoader.loadClientConfig(configFile)

            if (config.enabled) {
                val remoteDir = File(dataFolder, "remote")
                if (!remoteDir.exists()) {
                    remoteDir.mkdirs()
                }
            }
        }

        // 从dataFolder根目录读取手动放置的包（排除remote和.cache目录）
        val rootFiles = dataFolder.listFiles { file: File, name: String ->
            val child = File(file, name)
            (child.isDirectory && name != ".DS_Store" && name != "remote" && name != ".cache" && !name.startsWith(".")) ||
            name.endsWith(".zip") || name.endsWith(".mcpack") || name.endsWith(".mcaddon")
        } ?: emptyArray()

        // 从remote/目录读取远程下载的包（仅在客户端、启用远程同步且目录存在时）
        val remoteDir = File(dataFolder, "remote")
        val remoteFiles = if (envType == EnvType.CLIENT && remoteDir.exists() && remoteDir.isDirectory) {
            // 检查远程同步是否启用
            val configFile = File(dataFolder, "client.yml")
            val config = ClientConfigLoader.loadClientConfig(configFile)

            if (config.enabled) {
                // 远程同步启用，加载remote目录
                remoteDir.listFiles { file: File, name: String ->
                    val child = File(file, name)
                    (child.isDirectory && name != ".DS_Store" && !name.startsWith(".")) ||
                    name.endsWith(".zip") || name.endsWith(".mcpack") || name.endsWith(".mcaddon")
                } ?: emptyArray()
            } else {
                // 远程同步禁用，跳过remote目录
                BedrockLoader.logger.info("远程同步已禁用，跳过remote/目录的包加载")
                emptyArray()
            }
        } else {
            emptyArray()
        }

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
            val structureType = AddonScanner.detectStructureType(file)
            BedrockLoader.logger.info("  - ${file.name} [$structureType]")
        }
        BedrockLoader.logger.info("远程下载的包 (${remoteFiles.size}):")
        for (file in remoteFiles) {
            val structureType = AddonScanner.detectStructureType(file)
            BedrockLoader.logger.info("  - ${file.name} [$structureType]")
        }
        BedrockLoader.logger.info("========================================")

        BedrockLoader.logger.info("找到 ${rootFiles.size} 个手动放置的包, ${remoteFiles.size} 个远程下载的包")
        BedrockLoader.logger.info("加载顺序: 先远程包，后手动包（手动包优先覆盖）")

        // 加载所有包
        for (file in files) {
            try {
                val structureType = AddonScanner.detectStructureType(file)

                when (structureType) {
                    PackStructureType.SINGLE_PACK -> {
                        // 单个包目录
                        val zipFile = loadDirectoryPack(file, cacheDir) ?: continue
                        loadSinglePack(zipFile, isDirectoryPack = true, addonName = null)
                    }
                    PackStructureType.ADDON_DIRECTORY -> {
                        // Addon目录：整体打包为.mcaddon
                        val mcaddonFile = AddonScanner.packageAddonDirectory(file, cacheDir)
                        if (mcaddonFile != null) {
                            loadMcAddon(mcaddonFile, addonName = file.name)
                        }
                    }
                    PackStructureType.MCADDON_FILE -> {
                        // .mcaddon文件
                        loadMcAddon(file, addonName = file.nameWithoutExtension)
                    }
                    PackStructureType.MCPACK_FILE -> {
                        // .mcpack/.zip文件
                        loadSinglePack(file, isDirectoryPack = false, addonName = null)
                    }
                    PackStructureType.UNKNOWN -> {
                        BedrockLoader.logger.warn("无法识别的文件/目录: ${file.name}")
                    }
                }

            } catch (e: Exception) {
                BedrockLoader.logger.warn("Failed to load pack " + file.name, e)
            }
        }
    }

    /**
     * 加载.mcaddon文件中的所有包
     */
    private fun loadMcAddon(mcaddonFile: File, addonName: String) {
        BedrockLoader.logger.info("加载Addon: $addonName (${mcaddonFile.name})")

        val entries = AddonScanner.loadMcAddon(mcaddonFile)
        if (entries.isEmpty()) {
            BedrockLoader.logger.warn("Addon中未找到有效的包: $addonName")
            return
        }

        BedrockLoader.logger.info("  发现 ${entries.size} 个子包")

        ZipFile(mcaddonFile).use { zip ->
            for (entry in entries) {
                try {
                    loadPackFromMcAddonEntry(zip, entry, addonName, mcaddonFile)
                } catch (e: Exception) {
                    BedrockLoader.logger.warn("加载子包失败: ${entry.packPath}", e)
                }
            }
        }
    }

    /**
     * 从.mcaddon中加载单个子包
     */
    private fun loadPackFromMcAddonEntry(
        zip: ZipFile,
        entry: AddonScanner.McAddonEntry,
        addonName: String,
        mcaddonFile: File
    ) {
        val manifest = entry.manifest
        val packId = manifest.header?.uuid?.toString() ?: return
        val packName = manifest.header?.name ?: "Unknown"
        val packVersion = manifest.header?.version?.toString() ?: "0.0.0"
        val packType = manifest.modules.firstOrNull()?.type ?: "resources"

        // 创建PackInfo
        val packInfo = BedrockPackRegistry.PackInfo(
            id = packId,
            name = packName,
            version = packVersion,
            type = packType,
            file = mcaddonFile,
            md5 = MD5Util.calculateMD5(mcaddonFile),
            size = mcaddonFile.length(),
            manifest = manifest,
            addonName = addonName,
            isFromAddon = true
        )
        BedrockPackRegistry.register(packInfo)

        // 为该包创建独立的上下文
        var singleContext = context.packs.find { it.packId == packId }
        if (singleContext == null) {
            singleContext = net.easecation.bedrockloader.loader.context.SinglePackContext(packId, packInfo)
            context.packs.add(singleContext)
        }

        if (packType == "resources") {
            // 资源包
            resourcePackMap[packId] = object : net.easecation.bedrockloader.bedrock.pack.AbstractBedrockPack() {
                init {
                    this.id = packId
                    this.manifest = manifest
                    this.type = packType
                    this.version = packVersion
                }
            }

            val resourceContext = BedrockResourceDeserializer.deserialize(zip, entry.packPath)
            singleContext.resource.putAll(resourceContext)
            BedrockLoader.logger.info("  - 资源包: $packName [$packId] (${entry.packPath})")

        } else if (packType == "data") {
            // 行为包
            behaviourPackMap[packId] = object : net.easecation.bedrockloader.bedrock.pack.AbstractBedrockPack() {
                init {
                    this.id = packId
                    this.manifest = manifest
                    this.type = packType
                    this.version = packVersion
                }
            }

            val behaviorContext = BedrockBehaviorDeserializer.deserialize(zip, entry.packPath)
            singleContext.behavior.putAll(behaviorContext)
            BedrockLoader.logger.info("  - 行为包: $packName [$packId] (${entry.packPath})")
        }
    }

    /**
     * 加载单个包（ZIP文件）
     */
    private fun loadSinglePack(zipFile: File, isDirectoryPack: Boolean, addonName: String?) {
        val pack: BedrockPack = ZippedBedrockPack(zipFile)
        val packId = pack.getPackId() ?: return
        val packType = pack.getPackType()

        // 先创建PackInfo并注册到统一注册表
        val packInfo = createPackInfo(pack, zipFile, isDirectoryPack, addonName)
        BedrockPackRegistry.register(packInfo)

        if (packType.equals("resources")) {
            // 只添加到材质包管理器中
            resourcePackMap[packId] = pack

            // 为该包创建独立的资源上下文
            val resourceContext = BedrockResourceDeserializer.deserialize(ZipFile(zipFile))

            // 查找或创建对应的SinglePackContext
            var singleContext = context.packs.find { it.packId == packId }
            if (singleContext == null) {
                singleContext = net.easecation.bedrockloader.loader.context.SinglePackContext(packId, packInfo)
                context.packs.add(singleContext)
            }
            singleContext.resource.putAll(resourceContext)

            BedrockLoader.logger.info((("Reading resource pack: " + pack.getPackName()) + "[" + packId) + "]")
        } else if (packType.equals("data")) {
            // 行为包，需要读取内容
            behaviourPackMap[packId] = pack

            // 为该包创建独立的行为上下文
            val behaviorContext = BedrockBehaviorDeserializer.deserialize(ZipFile(zipFile))

            // 查找或创建对应的SinglePackContext
            var singleContext = context.packs.find { it.packId == packId }
            if (singleContext == null) {
                singleContext = net.easecation.bedrockloader.loader.context.SinglePackContext(packId, packInfo)
                context.packs.add(singleContext)
            }
            singleContext.behavior.putAll(behaviorContext)

            BedrockLoader.logger.info((("Reading behaviour pack: " + pack.getPackName()) + "[" + packId) + "]")
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
                hashFile.writeText(currentHash, Charsets.UTF_8)
                BedrockLoader.logger.debug("保存内容哈希: ${currentHash.substring(0, 8)}... -> ${hashFile.name}")
            } catch (e: Exception) {
                BedrockLoader.logger.warn("保存哈希文件失败: ${hashFile.name}", e)
            }

        } catch (e: java.io.IOException) {
            BedrockLoader.logger.error("无法打包文件夹资源包: ${directory.name}", e)
            return null
        }

        return cacheFile
    }

    /**
     * 创建包信息对象
     *
     * @param pack 基岩包对象
     * @param file ZIP文件
     * @param isDirectoryPack 是否是文件夹包
     * @param addonName 所属Addon名称（如果有）
     * @return PackInfo对象
     */
    private fun createPackInfo(
        pack: BedrockPack,
        file: File,
        isDirectoryPack: Boolean,
        addonName: String?
    ): BedrockPackRegistry.PackInfo {
        // 从BedrockPack获取包信息（已经解析好的）
        val packId = pack.getPackId() ?: throw IllegalStateException("包ID不能为空")
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

        // 返回包信息
        return BedrockPackRegistry.PackInfo(
            id = packId,
            name = packName,
            version = packVersion,
            type = packType,
            file = file,
            md5 = md5,
            size = size,
            manifest = manifest,
            addonName = addonName,
            isFromAddon = addonName != null
        )
    }

}
