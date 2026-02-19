package net.easecation.bedrockloader.loader.deserializer

import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityBehaviourDefinition
import net.easecation.bedrockloader.loader.InMemoryZipPack
import net.easecation.bedrockloader.loader.context.BedrockBehaviorContext
import net.easecation.bedrockloader.util.GsonUtil
import net.minecraft.util.Identifier
import java.io.InputStreamReader
import java.util.zip.ZipFile

object BedrockBehaviorDeserializer : PackDeserializer<BedrockBehaviorContext> {

    override fun deserialize(file: ZipFile): BedrockBehaviorContext {
        return deserialize(file, "")
    }

    override fun deserialize(file: ZipFile, pathPrefix: String): BedrockBehaviorContext {
        val context = BedrockBehaviorContext()

        val entries = file.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name

            // 如果有路径前缀，只处理以该前缀开头的文件
            if (pathPrefix.isNotEmpty() && !name.startsWith(pathPrefix)) {
                continue
            }

            // 移除前缀后的相对路径
            val relativeName = if (pathPrefix.isNotEmpty()) name.removePrefix(pathPrefix) else name

            if (relativeName.startsWith("entities/") && relativeName.endsWith(".json")) {
                file.getInputStream(entry).use { stream ->
                    val entityBehaviourDefinition: EntityBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityBehaviourDefinition::class.java)
                    val entityBehaviour: EntityBehaviourDefinition.EntityBehaviour = entityBehaviourDefinition.minecraftEntity
                    val identifier: Identifier = entityBehaviour.description.identifier
                    context.entities[identifier] = entityBehaviour
                }
            } else if (relativeName.startsWith("blocks/") && relativeName.endsWith(".json")) {
                file.getInputStream(entry).use { stream ->
                    val blockBehaviourDefinition: BlockBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockBehaviourDefinition::class.java)
                    val blockBehaviour: BlockBehaviourDefinition.BlockBehaviour = blockBehaviourDefinition.minecraftBlock
                    val identifier: Identifier = blockBehaviour.description.identifier
                    context.blocks[identifier] = blockBehaviour
                }
            }
        }
        return context
    }

    override fun deserialize(pack: InMemoryZipPack): BedrockBehaviorContext {
        return deserialize(pack, "")
    }

    override fun deserialize(pack: InMemoryZipPack, pathPrefix: String): BedrockBehaviorContext {
        val context = BedrockBehaviorContext()

        for (name in pack.entryNames) {
            if (pathPrefix.isNotEmpty() && !name.startsWith(pathPrefix)) continue

            val relativeName = if (pathPrefix.isNotEmpty()) name.removePrefix(pathPrefix) else name

            if (relativeName.startsWith("entities/") && relativeName.endsWith(".json")) {
                pack.getInputStream(name)?.use { stream ->
                    val entityBehaviourDefinition: EntityBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityBehaviourDefinition::class.java)
                    val entityBehaviour: EntityBehaviourDefinition.EntityBehaviour = entityBehaviourDefinition.minecraftEntity
                    val identifier: Identifier = entityBehaviour.description.identifier
                    context.entities[identifier] = entityBehaviour
                }
            } else if (relativeName.startsWith("blocks/") && relativeName.endsWith(".json")) {
                pack.getInputStream(name)?.use { stream ->
                    val blockBehaviourDefinition: BlockBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockBehaviourDefinition::class.java)
                    val blockBehaviour: BlockBehaviourDefinition.BlockBehaviour = blockBehaviourDefinition.minecraftBlock
                    val identifier: Identifier = blockBehaviour.description.identifier
                    context.blocks[identifier] = blockBehaviour
                }
            }
        }
        return context
    }

}