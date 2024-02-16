package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.easecation.bedrockloader.bedrock.pack.BedrockPack
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.TerrainTextureDefinition
import net.easecation.bedrockloader.bedrock.pack.ZippedBedrockPack
import net.easecation.bedrockloader.util.GsonUtil
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object BedrockAddonLoader {

    private val resourcePackMap: MutableMap<String, BedrockPack> = HashMap<String, BedrockPack>()

    private val behaviourPackMap: MutableMap<String, BedrockPack> = HashMap<String, BedrockPack>()

    // private val registeredEntities: MutableMap<String, EntityBehaviourFile.EntityBehaviour> = HashMap<String, EntityBehaviourFile.EntityBehaviour>()

    val registeredItems: MutableMap<Identifier, Item> = mutableMapOf()
    val registeredBlocks: MutableMap<Identifier, Block> = mutableMapOf()

    fun load() {
        val dataFolder: File = BedrockLoader.getGameDir().resolve("config/bedrock-loader")
        if (dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        // 从dataFolder中读取所有的zip文件
        val files = dataFolder.listFiles { dir: File?, name: String -> name.endsWith(".zip") || name.endsWith(".mcpack") }
        if (files == null) return
        if (files.isEmpty()) {
            BedrockLoader.logger.warn("No bedrock pack found in " + dataFolder.absolutePath)
            return
        }
        // 读取zip文件
        for (file in files) {
            BedrockLoader.logger.info("Loading pack... " + file.name)
            try {
                val pack = ZippedBedrockPack(file)
                if (pack.getPackType().equals("resources")) {
                    // 只添加到材质包管理器中
                    resourcePackMap[pack.getPackId()!!] = pack
                    this.loadResourcePack(pack, file)
                    BedrockLoader.logger.info((("Loaded resource pack: " + pack.getPackName()) + "[" + pack.getPackId()) + "]")
                } else if (pack.getPackType().equals("data")) {
                    // 行为包，需要读取内容
                    behaviourPackMap[pack.getPackId()!!] = pack
                    this.loadBehaviourPack(pack, file)
                    BedrockLoader.logger.info((("Loaded behaviour pack: " + pack.getPackName()) + "[" + pack.getPackId()) + "]")
                }
            } catch (e: Exception) {
                BedrockLoader.logger.warn("Failed to load pack " + file.name, e)
            }
        }
    }

    private fun loadResourcePack(pack: BedrockPack?, file: File) {
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: " + file.name)
        }
        val context = BedrockResourceContext()
        ZipFile(file).use { zip ->
            zip.getEntry("textures/terrain_texture.json")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val terrainTextureDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), TerrainTextureDefinition::class.java)
                    context.terrainTexture = terrainTextureDefinition.texture_data
                }
            }
            zip.getEntry("blocks.json")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val blockResourceDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockResourceDefinition::class.java)
                    context.blocks = blockResourceDefinition.blocks
                }
            }
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.startsWith("textures/") && (name.endsWith(".png") || name.endsWith(".jpg"))) {
                    val ext = name.substring(name.lastIndexOf('.') + 1)
                    val withoutExt = name.substring(0, name.lastIndexOf('.'))
                    context.textureImages[withoutExt] = TextureImage(ImageIO.read(zip.getInputStream(entry)), ext)
                }
            }
        }
        BedrockResourcePackLoader(BedrockLoader.getTmpResourceDir(), context).load()
    }

    private fun loadBehaviourPack(pack: BedrockPack?, file: File) {
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: " + file.name)
        }

        val context = BedrockBehaviorContext()

        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                /*if (name.startsWith("entities/") && name.endsWith(".json")) {
                    zip.getInputStream(entry).use { stream ->
                        val entityBehaviourFile: EntityBehaviourFile = GsonUtil.getGson().fromJson(InputStreamReader(stream), EntityBehaviourFile::class.java)
                        val entityBehaviour: EntityBehaviourFile.EntityBehaviour = entityBehaviourFile.minecraft_entity()
                        if (entityBehaviour != null) {
                            val identifier: String = entityBehaviour.description().identifier()
                            if (identifier != null) {
                                registeredEntities[identifier] = entityBehaviour
                                EntityDataDriven.ID_COMPONENTS_MAP.put(identifier, entityBehaviour.components())
                                Entity.registerEntity(
                                        identifier, identifier,
                                        EntityDataDriven::class.java
                                ) { chunk, nbt ->
                                    // 首次生成实体，写入identifier
                                    if (!nbt.contains("identifier")) {
                                        nbt.putString("identifier", identifier)
                                    }
                                    EntityDataDriven(chunk, nbt)
                                }
                                plugin.getLogger().info("[CustomEntity] 注册实体：$identifier")
                            }
                        }
                    }
                } else */if (name.startsWith("blocks/") && name.endsWith(".json")) {
                    zip.getInputStream(entry).use { stream ->
                        val blockBehaviourDefinition: BlockBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockBehaviourDefinition::class.java)
                        val blockBehaviour: BlockBehaviourDefinition.BlockBehaviour = blockBehaviourDefinition.minecraftBlock
                        val identifier: Identifier = blockBehaviour.description.identifier
                        // registeredBlocks[identifier] = blockBehaviour
                        context.blocks[identifier] = blockBehaviour
                    }
                }
            }
        }

        val loader = BedrockBehaviorPackLoader(context)
        loader.load()

        registeredBlocks.putAll(loader.registeredBlocks)
        registeredItems.putAll(loader.registeredItems)
    }
}