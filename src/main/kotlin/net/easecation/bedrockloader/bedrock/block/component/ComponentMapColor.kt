package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * 基岩版 minecraft:map_color 组件
 * 用于设置方块在地图上显示的颜色
 *
 * 支持三种格式:
 * 1. 十六进制字符串: "#5f4a2b"
 * 2. RGB 数组: [95, 74, 43]
 * 3. 对象格式: { "color": "#639f28", "tint_method": "default_foliage" }
 */
sealed class ComponentMapColor : IBlockComponent {

    /**
     * 获取 RGB 颜色值 (0xRRGGBB 格式)
     */
    abstract fun getColor(): Int

    /**
     * 格式1: 十六进制字符串 "#RRGGBB"
     */
    data class Hex(val color: String) : ComponentMapColor() {
        override fun getColor(): Int = color.removePrefix("#").toInt(16)
    }

    /**
     * 格式2: RGB 数组 [R, G, B]
     */
    data class Rgb(val color: List<Int>) : ComponentMapColor() {
        override fun getColor(): Int = (color[0] shl 16) or (color[1] shl 8) or color[2]
    }

    /**
     * 格式3: 对象格式 { "color": "#...", "tint_method": "..." }
     * 注: tint_method 暂不支持，需要生物群系感知的动态颜色
     */
    data class Full(
        val color: String,
        @SerializedName("tint_method") val tintMethod: String? = null
    ) : ComponentMapColor() {
        override fun getColor(): Int = color.removePrefix("#").toInt(16)
    }

    /**
     * 自定义反序列化器，自动判断 JSON 格式
     */
    class Deserializer : JsonDeserializer<ComponentMapColor> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentMapColor {
            return when {
                // 格式1: 十六进制字符串
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    Hex(json.asString)
                }
                // 格式2: RGB 数组
                json.isJsonArray -> {
                    val array = json.asJsonArray
                    if (array.size() != 3) {
                        throw JsonParseException("map_color RGB array must have exactly 3 elements, got ${array.size()}")
                    }
                    Rgb(array.map { it.asInt })
                }
                // 格式3: 对象格式
                json.isJsonObject -> {
                    context.deserialize(json, Full::class.java)
                }
                else -> throw JsonParseException("Unknown map_color format: $json")
            }
        }
    }
}
