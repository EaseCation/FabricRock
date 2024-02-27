package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

sealed class ComponentCollisionBox : IBlockComponent {

    data class ComponentCollisionBoxBoolean(
            val value: Boolean
    ) : ComponentCollisionBox()

    data class ComponentCollisionBoxCustom(
            val origin : FloatArray,
            val size : FloatArray
    ) : ComponentCollisionBox() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ComponentCollisionBoxCustom

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

    class Deserializer : JsonDeserializer<ComponentCollisionBox> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentCollisionBox {
            return if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
                ComponentCollisionBoxBoolean(json.asBoolean)
            } else {
                val obj = json.asJsonObject
                val origin = obj["origin"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
                val size = obj["size"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
                ComponentCollisionBoxCustom(origin, size)
            }
        }
    }

}
