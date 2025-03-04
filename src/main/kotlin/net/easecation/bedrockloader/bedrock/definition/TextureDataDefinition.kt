package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import net.easecation.bedrockloader.bedrock.BedrockTexturePath
import java.lang.reflect.Type

data class TextureDataDefinition(
    val textures: List<TextureEntry>
) {
    class Deserializer : JsonDeserializer<TextureDataDefinition> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TextureDataDefinition {
            if (!json.isJsonObject) throw JsonParseException("Unexpected JSON type for TextureDataDefinition")
            val textures = json.asJsonObject["textures"] ?: throw JsonParseException("Missing 'textures' in TextureDataDefinition")
            return TextureDataDefinition(when {
                textures.isJsonArray -> textures.asJsonArray.map { deserializeEntry(it, context) }
                else -> listOf(deserializeEntry(textures, context))
            })
        }

        private fun deserializeEntry(json: JsonElement, context: JsonDeserializationContext): TextureEntry {
            return when {
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> TextureEntry(json.asString, null)
                json.isJsonObject -> context.deserialize(json, TextureEntry::class.java)
                else -> throw JsonParseException("Unexpected JSON type for TextureEntry")
            }
        }
    }

    data class TextureEntry(
        val path: BedrockTexturePath,
        val overlay_color: String?
    )
}