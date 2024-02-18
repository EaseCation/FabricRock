package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

sealed class ComponentGeometry : IBlockComponent {

    data class ComponentGeometrySimple(val identifier: String) : ComponentGeometry()

    data class ComponentGeometryFull(val identifier: String,
                                     val bone_visibility: Map<String, Boolean>?
    ) : ComponentGeometry()

    class Deserializer : JsonDeserializer<ComponentGeometry> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentGeometry {
            return if (json.isJsonObject) {
                val type = object : TypeToken<ComponentGeometryFull?>() {}.type
                context.deserialize<ComponentGeometryFull>(json, type)
            } else if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
                // 简单字符串处理
                ComponentGeometrySimple(json.asString)
            } else {
                throw JsonParseException("Unexpected JSON type for ComponentGeometry")
            }
        }
    }


}