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
         *
         * 支持 pre/post 格式实现 step 动画效果：
         * - pre: 进入该时间点前的目标值（从前一帧插值到此）
         * - post: 离开该时间点后的起始值（从此插值到下一帧）
         * - 在该时间点瞬间从 pre 跳变到 post
         */
        data class Keyframe(
            val time: Double,
            val pre: List<Double>,
            val post: List<Double>
        ) {
            /** 是否为阶跃关键帧（pre 和 post 不同） */
            val isStep: Boolean get() = pre != post

            companion object {
                /** 创建普通关键帧（pre = post） */
                fun simple(time: Double, value: List<Double>) = Keyframe(time, value, value)
            }
        }

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
                            val (pre, post) = parseKeyframeValue(value)
                            Keyframe(time, pre, post)
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
             * 解析关键帧值，返回 (pre, post) 对
             *
             * 支持的格式：
             * - 单值: 45.0 → pre=post=[45,45,45]
             * - 数组: [x,y,z] → pre=post=[x,y,z]
             * - pre/post对象: {"pre":[...], "post":[...]} → 分别解析
             * - 只有pre: {"pre":[...]} → post=pre
             * - 只有post: {"post":[...]} → pre=post
             */
            private fun parseKeyframeValue(json: JsonElement): Pair<List<Double>, List<Double>> {
                return when {
                    // 单个数字: 45.0
                    json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                        val v = json.asDouble
                        val vec = listOf(v, v, v)
                        Pair(vec, vec)
                    }
                    // Molang 字符串
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                        val v = parseMolangValue(json.asString)
                        val vec = listOf(v, v, v)
                        Pair(vec, vec)
                    }
                    // 数组: [x, y, z]
                    json.isJsonArray -> {
                        val vec = json.asJsonArray.map { parseNumericValue(it) }
                        Pair(vec, vec)
                    }
                    // 对象: 可能是 pre/post 格式
                    json.isJsonObject -> {
                        val obj = json.asJsonObject
                        val preElement = obj["pre"]
                        val postElement = obj["post"]

                        when {
                            // 同时有 pre 和 post
                            preElement != null && postElement != null -> {
                                val pre = parseVector(preElement)
                                val post = parseVector(postElement)
                                Pair(pre, post)
                            }
                            // 只有 pre
                            preElement != null -> {
                                val pre = parseVector(preElement)
                                Pair(pre, pre)
                            }
                            // 只有 post
                            postElement != null -> {
                                val post = parseVector(postElement)
                                Pair(post, post)
                            }
                            // 都没有（异常情况）
                            else -> {
                                val default = listOf(0.0, 0.0, 0.0)
                                Pair(default, default)
                            }
                        }
                    }
                    else -> {
                        val default = listOf(0.0, 0.0, 0.0)
                        Pair(default, default)
                    }
                }
            }

            /**
             * 解析向量值（支持多种格式）
             *
             * 格式：
             * - 单值: 45.0 → [45, 45, 45]
             * - 数组: [x, y, z]
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
                    else -> listOf(0.0, 0.0, 0.0)
                }
            }
        }
    }
}
