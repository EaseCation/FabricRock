package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.*
import net.easecation.bedrockloader.BedrockLoader
import java.lang.reflect.Type

/**
 * 基岩版动画文件定义
 *
 * 动画文件格式示例：
 * ```json
 * {
 *   "format_version": "1.8.0",
 *   "animations": {
 *     "animation.entity.idle": {
 *       "loop": true,
 *       "animation_length": 3.0,
 *       "bones": {
 *         "bone_name": {
 *           "rotation": { "0.0": [0, 0, 0], "1.5": [0, 0, -2.5] },
 *           "position": [0, 1, 0],
 *           "scale": 1.5
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 */
data class AnimationDefinition(
    val format_version: String,
    val animations: Map<String, Animation>
) {
    /**
     * 单个动画定义
     */
    data class Animation(
        val loop: LoopMode,
        val animation_length: Double?,
        val bones: Map<String, BoneAnimation>?
    )

    /**
     * 循环模式
     */
    sealed class LoopMode {
        /** 循环播放 (loop: true) */
        object Loop : LoopMode()
        /** 单次播放 (loop: false 或未指定) */
        object Once : LoopMode()
        /** 停止在最后一帧 (loop: "hold_on_last_frame") */
        object HoldOnLastFrame : LoopMode()

        override fun toString(): String = when (this) {
            is Loop -> "Loop"
            is Once -> "Once"
            is HoldOnLastFrame -> "HoldOnLastFrame"
        }

        /**
         * Gson 反序列化器
         */
        class Deserializer : JsonDeserializer<LoopMode> {
            override fun deserialize(
                json: JsonElement?,
                typeOfT: Type?,
                context: JsonDeserializationContext?
            ): LoopMode {
                if (json == null || json.isJsonNull) {
                    return Once  // 默认值
                }
                return when {
                    json.isJsonPrimitive -> {
                        val primitive = json.asJsonPrimitive
                        when {
                            primitive.isBoolean -> if (primitive.asBoolean) Loop else Once
                            primitive.isString -> {
                                when (primitive.asString.lowercase()) {
                                    "true" -> Loop
                                    "false" -> Once
                                    "hold_on_last_frame" -> HoldOnLastFrame
                                    else -> Once
                                }
                            }
                            else -> Once
                        }
                    }
                    else -> Once
                }
            }
        }
    }

    /**
     * 骨骼动画数据
     */
    data class BoneAnimation(
        val rotation: AnimationChannel?,
        val position: AnimationChannel?,
        val scale: AnimationChannel?
    ) {
        /**
         * Gson 反序列化器
         */
        class Deserializer : JsonDeserializer<BoneAnimation> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): BoneAnimation {
                val obj = json.asJsonObject
                return BoneAnimation(
                    rotation = obj["rotation"]?.let { AnimationChannel.parse(it) },
                    position = obj["position"]?.let { AnimationChannel.parse(it) },
                    scale = obj["scale"]?.let { AnimationChannel.parse(it) }
                )
            }
        }
    }

    /**
     * 动画通道（支持多种格式）
     *
     * 支持的格式：
     * - 单值: 45.0 → [45, 45, 45]
     * - 数组: [x, y, z]
     * - 关键帧Map: {"0.0": [x,y,z], "0.5": [x,y,z]}
     * - Molang表达式: "math.sin(...)" → 暂时解析为0并警告
     */
    sealed class AnimationChannel {
        /** 固定值 [x, y, z] */
        data class Static(val value: List<Double>) : AnimationChannel()

        /** 关键帧动画 */
        data class Keyframes(val frames: List<Keyframe>) : AnimationChannel()

        /**
         * 单个关键帧
         */
        data class Keyframe(
            val time: Double,
            val value: List<Double>
        )

        companion object {
            /**
             * 解析动画通道值
             */
            fun parse(json: JsonElement): AnimationChannel {
                return when {
                    // 单个数字: 45.0 → [45, 45, 45]
                    json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                        val v = json.asDouble
                        Static(listOf(v, v, v))
                    }
                    // Molang字符串: "math.sin(...)" → 尝试解析为数字，失败返回0
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                        val str = json.asString
                        val v = parseMolangValue(str)
                        Static(listOf(v, v, v))
                    }
                    // 数组: [x, y, z]
                    json.isJsonArray -> {
                        val arr = json.asJsonArray.map { parseNumericValue(it) }
                        Static(arr)
                    }
                    // 关键帧Map: {"0.0": [...], "0.5": [...]}
                    json.isJsonObject -> {
                        val keyframes = json.asJsonObject.entrySet().mapNotNull { (timeStr, value) ->
                            val time = timeStr.toDoubleOrNull()
                            if (time == null) {
                                BedrockLoader.logger.warn("[AnimationDefinition] Invalid keyframe time: $timeStr")
                                return@mapNotNull null
                            }
                            val vec = parseVector(value)
                            Keyframe(time, vec)
                        }.sortedBy { it.time }
                        Keyframes(keyframes)
                    }
                    else -> Static(listOf(0.0, 0.0, 0.0))
                }
            }

            /**
             * 解析可能是Molang的数值
             * 当前实现：尝试解析为数字，失败返回0并警告
             * 未来扩展：实现完整Molang解析器
             */
            private fun parseMolangValue(str: String): Double {
                return str.toDoubleOrNull() ?: run {
                    BedrockLoader.logger.warn("[AnimationDefinition] Molang not supported, treating as 0: $str")
                    0.0
                }
            }

            /**
             * 解析单个数值元素
             */
            private fun parseNumericValue(json: JsonElement): Double {
                return when {
                    json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> json.asDouble
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> parseMolangValue(json.asString)
                    else -> 0.0
                }
            }

            /**
             * 解析向量值（支持多种格式）
             *
             * 格式：
             * - 单值: 45.0 → [45, 45, 45]
             * - 数组: [x, y, z]
             * - 对象（pre/post）: {"pre": [...], "post": [...]} → 使用post值
             */
            private fun parseVector(json: JsonElement): List<Double> {
                return when {
                    json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                        val v = json.asDouble
                        listOf(v, v, v)
                    }
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                        val v = parseMolangValue(json.asString)
                        listOf(v, v, v)
                    }
                    json.isJsonArray -> {
                        json.asJsonArray.map { parseNumericValue(it) }
                    }
                    json.isJsonObject -> {
                        // pre/post 格式，优先使用 post
                        val obj = json.asJsonObject
                        val postValue = obj["post"] ?: obj["pre"]
                        if (postValue != null) {
                            parseVector(postValue)
                        } else {
                            listOf(0.0, 0.0, 0.0)
                        }
                    }
                    else -> listOf(0.0, 0.0, 0.0)
                }
            }
        }
    }
}
