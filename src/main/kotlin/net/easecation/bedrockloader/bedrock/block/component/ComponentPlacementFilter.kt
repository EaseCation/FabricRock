package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.*

data class ComponentPlacementFilter(
        val conditions: List<Condition>
) : IBlockComponent {
    data class Condition(
            val allowedFaces: List<AllowedFaces>?,
            val blockFilter: List<BlockDescriptor>?
    )

    enum class AllowedFaces {
        UP, DOWN, NORTH, SOUTH, EAST, WEST, SIDE, ALL
    }

    sealed class BlockDescriptor

    data class BlockDescriptorWithName(
            val name: String
    ) : BlockDescriptor()

    data class BlockDescriptorWithData(
            val name: String,
            val states: List<String>,
            val tags: String
    ) : BlockDescriptor()

    class Deserializer : JsonDeserializer<ComponentPlacementFilter> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentPlacementFilter {
            val obj = json.asJsonObject
            val conditions = obj.getAsJsonArray("conditions").map { it ->
                val condition = it.asJsonObject
                val allowedFaces = condition.getAsJsonArray("allowed_faces")?.map { AllowedFaces.valueOf(it.asString.uppercase(Locale.getDefault())) }
                val blockFilter = condition.getAsJsonArray("block_filter")?.map {
                    if (it.isJsonObject) {
                        val block = it.asJsonObject
                        BlockDescriptorWithData(
                                block.get("name").asString,
                                block.getAsJsonArray("states").map { it.asString },
                                block.get("tags").asString
                        )
                    } else if (it.isJsonPrimitive) {
                        BlockDescriptorWithName(it.asString)
                    } else {
                        throw IllegalArgumentException("Unexpected JSON type for BlockDescriptor")
                    }
                }
                Condition(allowedFaces, blockFilter)
            }
            return ComponentPlacementFilter(conditions)
        }
    }
}