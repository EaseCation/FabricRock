package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

sealed class ComponentGeometry : IBlockComponent {

    data class ComponentGeometrySimple(val identifier: String) : ComponentGeometry()

    data class ComponentGeometryFull(
        val identifier: String,
        val bone_visibility: Map<String, Boolean>?,
        val culling: String?  // 剔除规则标识符，引用 block_culling/ 中定义的规则
    ) : ComponentGeometry()

    /**
     * 获取几何体标识符
     */
    fun geometryIdentifier(): String {
        return when (this) {
            is ComponentGeometrySimple -> identifier
            is ComponentGeometryFull -> identifier
        }
    }

    /**
     * 获取剔除规则标识符
     */
    fun getCullingIdentifier(): String? {
        return when (this) {
            is ComponentGeometrySimple -> null
            is ComponentGeometryFull -> culling
        }
    }

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