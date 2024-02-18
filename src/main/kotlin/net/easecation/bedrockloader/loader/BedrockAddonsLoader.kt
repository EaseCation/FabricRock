package net.easecation.bedrockloader.loader

import com.google.common.io.Files
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.pack.BedrockPack
import net.easecation.bedrockloader.bedrock.pack.ZippedBedrockPack
import net.easecation.bedrockloader.deserializer.BedrockBehaviorDeserializer
import net.easecation.bedrockloader.deserializer.BedrockPackContext
import net.easecation.bedrockloader.deserializer.BedrockResourceDeserializer
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
        if (dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        // 从dataFolder中读取所有的zip文件
        val files = dataFolder.listFiles { file: File, name: String -> (file.isDirectory && name != ".DS_Store") || name.endsWith(".zip") || name.endsWith(".mcpack") }
        if (files == null) return
        if (files.isEmpty()) {
            BedrockLoader.logger.warn("No bedrock pack found in " + dataFolder.absolutePath)
            return
        }
        // 读取zip文件
        for (file in files) {
            try {
                var f: File = file
                if (file.isDirectory) {
                    loadDirectoryPack(file)?.let { f = it } ?: continue
                }
                val pack: BedrockPack = ZippedBedrockPack(f)
                if (pack.getPackType().equals("resources")) {
                    // 只添加到材质包管理器中
                    resourcePackMap[pack.getPackId()!!] = pack
                    context.resource.putAll(BedrockResourceDeserializer.deserialize(ZipFile(f)))
                    BedrockLoader.logger.info((("Reading resource pack: " + pack.getPackName()) + "[" + pack.getPackId()) + "]")
                } else if (pack.getPackType().equals("data")) {
                    // 行为包，需要读取内容
                    behaviourPackMap[pack.getPackId()!!] = pack
                    context.behavior.putAll(BedrockBehaviorDeserializer.deserialize(ZipFile(f)))
                    BedrockLoader.logger.info((("Reading behaviour pack: " + pack.getPackName()) + "[" + pack.getPackId()) + "]")
                }
            } catch (e: Exception) {
                BedrockLoader.logger.warn("Failed to load pack " + file.name, e)
            }
        }

        // load resource pack
        BedrockLoader.logger.info("Loading resource pack...")
        BedrockResourcePackLoader(BedrockLoader.getTmpResourceDir(), context).load()

        // load behaviour pack
        BedrockLoader.logger.info("Loading behaviour pack...")
        val loader = BedrockBehaviorPackLoader(context)
        loader.load()

        BedrockLoader.logger.info("Loading pack finished! ${BedrockAddonsRegistry.blocks.size} blocks, ${BedrockAddonsRegistry.items.size} items, ${BedrockAddonsRegistry.entities.size} entities")
    }

    private fun loadDirectoryPack(directory: File): File? {
        val manifestFile = File(directory, "manifest.json")
        if (!manifestFile.exists() || !manifestFile.isFile) {
            return null
        }

        val tempFile: File
        try {
            tempFile = File.createTempFile("pack", ".zip")
            tempFile.deleteOnExit()

            val time = FileTime.fromMillis(0)
            ZipOutputStream(FileOutputStream(tempFile)).use { stream ->
                stream.setLevel(Deflater.BEST_COMPRESSION)
                val files: Collection<File> = TreeSet(FileUtils.listFiles(directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE))
                for (file in files) {
                    val entry = ZipEntry(directory.toPath().relativize(file.toPath()).toString())
                            .setCreationTime(time)
                            .setLastModifiedTime(time)
                            .setLastAccessTime(time)
                    stream.putNextEntry(entry)
                    stream.write(Files.toByteArray(file))
                    stream.closeEntry()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to create temporary mcpack file", e)
        }
        return tempFile
    }

}