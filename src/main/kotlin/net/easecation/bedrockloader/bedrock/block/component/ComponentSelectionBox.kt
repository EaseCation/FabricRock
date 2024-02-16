package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

sealed class ComponentSelectionBox : IBlockComponent {

    data class ComponentSelectionBoxWithBoolean(
            val value: Boolean
    ) : ComponentSelectionBox()

    data class ComponentSelectionBoxWithData(
            val origin : FloatArray,
            val size : FloatArray
    ) : ComponentSelectionBox() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ComponentSelectionBoxWithData

            if (!origin.contentEquals(other.origin)) return false
            if (!size.contentEquals(other.size)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = origin.contentHashCode()
            result = 31 * result + size.contentHashCode()
            return result
        }

    }

    class Deserializer : JsonDeserializer<ComponentSelectionBox> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentSelectionBox {
            return if (json.isJsonObject) {
                if (json.asJsonObject.has("value")) {
                    val type = object : TypeToken<ComponentSelectionBoxWithBoolean?>() {}.type
                    context.deserialize<ComponentSelectionBoxWithBoolean>(json, type)
                } else {
                    val type = object : TypeToken<ComponentSelectionBoxWithData?>() {}.type
                    context.deserialize<ComponentSelectionBoxWithData>(json, type)
                }
                val type = object : TypeToken<ComponentSelectionBoxWithData?>() {}.type
                context.deserialize<ComponentSelectionBoxWithData>(json, type)
            } else if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
                // 简单字符串处理
                ComponentSelectionBoxWithBoolean(json.asBoolean)
            } else {
                throw JsonParseException("Unexpected JSON type for ComponentSelectionBox")
            }
        }
    }

}
