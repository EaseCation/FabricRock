package net.easecation.bedrockloader.animation

import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition
import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * 骨骼变换数据
 */
@Environment(EnvType.CLIENT)
data class BoneTransform(
    val rotation: List<Double>?,   // [pitch, yaw, roll] 角度（度数）
    val position: List<Double>?,   // [x, y, z]
    val scale: List<Double>?       // [x, y, z]
)

/**
 * 动画播放器 - 管理单个动画的播放状态
 *
 * 支持的功能：
 * - 循环播放 (loop: true)
 * - 单次播放 (loop: false)
 * - 停止在最后一帧 (loop: "hold_on_last_frame")
 * - 长度为0的循环动画
 * - 线性关键帧插值
 */
@Environment(EnvType.CLIENT)
class AnimationPlayer(
    private val animation: Animation
) {
    private var currentTime: Double = 0.0
    private var isPlaying: Boolean = true
    private var isFinished: Boolean = false

    /** 获取动画长度 */
    val animationLength: Double
        get() = animation.animation_length ?: calculateMaxKeyframeTime()

    /** 计算最大关键帧时间 */
    private fun calculateMaxKeyframeTime(): Double {
        var maxTime = 0.0
        animation.bones?.values?.forEach { bone ->
            listOfNotNull(bone.rotation, bone.position, bone.scale).forEach { channel ->
                if (channel is AnimationChannel.Keyframes) {
                    channel.frames.lastOrNull()?.let {
                        maxTime = maxOf(maxTime, it.time)
                    }
                }
            }
        }
        return maxTime
    }

    /**
     * 更新动画时间
     * @param deltaTime 时间增量（秒）
     */
    fun tick(deltaTime: Double) {
        if (!isPlaying || isFinished) return

        currentTime += deltaTime

        when (animation.loop) {
            is LoopMode.Loop -> {
                // 特殊处理长度为0的循环动画
                if (animationLength <= 0) {
                    currentTime = 0.0
                } else {
                    while (currentTime >= animationLength) {
                        currentTime -= animationLength
                    }
                }
            }
            is LoopMode.Once -> {
                if (currentTime >= animationLength) {
                    currentTime = animationLength
                    isFinished = true
                }
            }
            is LoopMode.HoldOnLastFrame -> {
                if (currentTime >= animationLength) {
                    currentTime = animationLength
                    // 不标记finished，保持在最后一帧
                }
            }
        }
    }

    /**
     * 获取指定骨骼在当前时间的变换值
     */
    fun getBoneTransform(boneName: String): BoneTransform? {
        val boneAnim = animation.bones?.get(boneName) ?: return null
        return BoneTransform(
            rotation = interpolateChannel(boneAnim.rotation, currentTime),
            position = interpolateChannel(boneAnim.position, currentTime),
            scale = interpolateChannel(boneAnim.scale, currentTime)
        )
    }

    /**
     * 获取所有有动画的骨骼名称
     */
    fun getAnimatedBoneNames(): Set<String> {
        return animation.bones?.keys ?: emptySet()
    }

    /**
     * 线性插值通道值
     */
    private fun interpolateChannel(channel: AnimationChannel?, time: Double): List<Double>? {
        return when (channel) {
            null -> null
            is AnimationChannel.Static -> channel.value
            is AnimationChannel.Keyframes -> interpolateKeyframes(channel.frames, time)
        }
    }

    /**
     * 关键帧插值（支持 pre/post step 效果）
     *
     * 插值语义：
     * - 从前一帧的 post 值开始
     * - 线性插值到下一帧的 pre 值
     * - 在下一帧时间点瞬间跳变到 post 值（step 效果）
     */
    private fun interpolateKeyframes(
        frames: List<AnimationChannel.Keyframe>,
        time: Double
    ): List<Double> {
        if (frames.isEmpty()) return listOf(0.0, 0.0, 0.0)
        if (frames.size == 1) return frames[0].post  // 单帧使用 post 值

        // 找到当前时间所在区间
        val nextIndex = frames.indexOfFirst { it.time > time }

        return when {
            // 超过最后一帧：返回最后一帧的 post 值
            nextIndex == -1 -> frames.last().post
            // 在第一帧之前：返回第一帧的 pre 值
            nextIndex == 0 -> frames.first().pre
            // 正常插值区间
            else -> {
                val prev = frames[nextIndex - 1]
                val next = frames[nextIndex]
                // 关键改变：从 prev.post 插值到 next.pre
                // 在 next.time 这一刻会瞬间跳变到 next.post
                val t = (time - prev.time) / (next.time - prev.time)
                lerp(prev.post, next.pre, t)
            }
        }
    }

    /**
     * 向量线性插值
     */
    private fun lerp(a: List<Double>, b: List<Double>, t: Double): List<Double> {
        // 确保两个向量长度相同
        val maxLen = maxOf(a.size, b.size)
        val aExtended = a + List(maxLen - a.size) { 0.0 }
        val bExtended = b + List(maxLen - b.size) { 0.0 }
        return aExtended.zip(bExtended) { av, bv -> av + (bv - av) * t }
    }

    /**
     * 重置动画到初始状态
     */
    fun reset() {
        currentTime = 0.0
        isFinished = false
        isPlaying = true
    }

    /**
     * 暂停动画
     */
    fun pause() {
        isPlaying = false
    }

    /**
     * 恢复动画
     */
    fun resume() {
        isPlaying = true
    }

    /**
     * 获取当前播放时间
     */
    fun getCurrentTime(): Double = currentTime

    /**
     * 设置当前播放时间
     */
    fun setCurrentTime(time: Double) {
        currentTime = time.coerceIn(0.0, animationLength)
        if (currentTime < animationLength) {
            isFinished = false
        }
    }

    /**
     * 动画是否正在播放
     */
    fun isPlaying(): Boolean = isPlaying && !isFinished

    /**
     * 动画是否已结束
     */
    fun isFinished(): Boolean = isFinished
}
