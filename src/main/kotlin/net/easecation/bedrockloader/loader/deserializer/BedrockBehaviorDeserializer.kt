package net.easecation.bedrockloader.loader.deserializer

import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityBehaviourDefinition
import net.easecation.bedrockloader.loader.context.BedrockBehaviorContext
import net.easecation.bedrockloader.util.GsonUtil
import net.minecraft.util.Identifier
import java.io.InputStreamReader
import java.util.zip.ZipFile

object BedrockBehaviorDeserializer : PackDeserializer<BedrockBehaviorContext> {

    override fun deserialize(file: ZipFile): BedrockBehaviorContext {
        val context = BedrockBehaviorContext()

        file.use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.startsWith("entities/") && name.endsWith(".json")) {
                    zip.getInputStream(entry).use { stream ->
                        val entityBehaviourDefinition: EntityBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityBehaviourDefinition::class.java)
                        val entityBehaviour: EntityBehaviourDefinition.EntityBehaviour = entityBehaviourDefinition.minecraftEntity
                        val identifier: Identifier = entityBehaviour.description.identifier
                        context.entities[identifier] = entityBehaviour
                    }
                } else if (name.startsWith("blocks/") && name.endsWith(".json")) {
                    zip.getInputStream(entry).use { stream ->
                        val blockBehaviourDefinition: BlockBehaviourDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockBehaviourDefinition::class.java)
                        val blockBehaviour: BlockBehaviourDefinition.BlockBehaviour = blockBehaviourDefinition.minecraftBlock
                        val identifier: Identifier = blockBehaviour.description.identifier
                        context.blocks[identifier] = blockBehaviour
                    }
                }
            }
        }
        return context
    }

}