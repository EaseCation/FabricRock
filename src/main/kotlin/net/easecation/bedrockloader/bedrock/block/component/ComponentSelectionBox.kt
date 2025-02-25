package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

sealed class ComponentSelectionBox : IBlockComponent {

    data class ComponentSelectionBoxBoolean(
            val value: Boolean
    ) : ComponentSelectionBox()

    data class ComponentSelectionBoxCustom(
            val origin : FloatArray,
            val size : FloatArray
    ) : ComponentSelectionBox() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ComponentSelectionBoxCustom

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
            return if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
                ComponentSelectionBoxBoolean(json.asBoolean)
            } else {
                val obj = json.asJsonObject
                val origin = obj["origin"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
                val size = obj["size"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
                ComponentSelectionBoxCustom(origin, size)
            }
        }
    }

}
