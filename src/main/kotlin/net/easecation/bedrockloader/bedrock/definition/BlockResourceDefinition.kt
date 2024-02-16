package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.minecraft.util.Identifier
import java.lang.reflect.Type

data class BlockResourceDefinition(
        val format_version: String,
        val blocks: Map<Identifier, Block>
) {

    data class Block(
            val textures: Textures,
            val carried_textures: String?,
            val sound: String?
    )

    class Deserializer : JsonDeserializer<BlockResourceDefinition> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BlockResourceDefinition {
            val obj = json.asJsonObject
            val blocks = mutableMapOf<Identifier, Block>()
            for ((key, value) in obj.entrySet()) {
                if (key == "format_version") continue
                val identifier = Identifier(key)
                blocks[identifier] = context.deserialize(value, Block::class.java)
            }
            return BlockResourceDefinition(
                    obj["format_version"].asString,
                    blocks
            )
        }
    }

    sealed class Textures {

        data class TexturesAllFace(
                val all: String
        ) : Textures()

        data class TexturesMultiFace(
                val up: String?,
                val down: String?,
                val north: String?,
                val south: String?,
                val east: String?,
                val west: String?
        ) : Textures()

        class Deserializer : JsonDeserializer<Textures> {
            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Textures {
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    return TexturesMultiFace(
                            obj["up"]?.asString,
                            obj["down"]?.asString,
                            obj["north"]?.asString,
                            obj["south"]?.asString,
                            obj["east"]?.asString,
                            obj["west"]?.asString
                    )
                } else {
                    return TexturesAllFace(json.asString)
                }
            }
        }

    }

}