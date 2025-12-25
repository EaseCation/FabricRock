package net.easecation.bedrockloader.animation

import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * 实体动画管理器 - 管理实体的所有动画播放器
 *
 * 每个实体实例持有一个独立的 EntityAnimationManager，
 * 确保动画状态不会在实体之间同步。
 *
 * 功能：
 * - 管理多个 AnimationPlayer
 * - 处理 scripts.animate 自动播放
 * - 合并多个动画的骨骼变换
 */
@Environment(EnvType.CLIENT)
class EntityAnimationManager(
    private val animationMap: Map<String, String>,           // 别名 -> 动画ID
    private val animations: Map<String, AnimationDefinition.Animation>,  // 动画ID -> 动画数据
    private val autoPlayList: List<String>                   // scripts.animate 列表（别名）
) {
    private val players = mutableMapOf<String, AnimationPlayer>()
    private var lastUpdateTime: Long = 0L  // 上次更新时间（纳秒）

    init {
        // 为 scripts.animate 中的动画创建播放器
        autoPlayList.forEach { alias ->
            val animId = animationMap[alias] ?: return@forEach
            val anim = animations[animId] ?: return@forEach
            players[alias] = AnimationPlayer(anim)
        }
    }

    /**
     * 每帧更新所有动画播放器
     * 使用系统时间计算真实的帧间隔
     */
    fun tick() {
        val currentTime = System.nanoTime()
        val deltaTime = if (lastUpdateTime == 0L) {
            0.05  // 首次调用使用默认值（1 游戏 tick）
        } else {
            (currentTime - lastUpdateTime) / 1_000_000_000.0  // 纳秒转秒
        }
        lastUpdateTime = currentTime

        players.values.forEach { it.tick(deltaTime) }
    }

    /**
     * 获取指定骨骼的合并变换
     * 多个动画的变换会叠加（旋转和位移相加，缩放相乘）
     */
    fun getBoneTransform(boneName: String): BoneTransform? {
        var result: BoneTransform? = null

        for (player in players.values) {
            val transform = player.getBoneTransform(boneName) ?: continue
            result = if (result == null) {
                transform
            } else {
                // 叠加变换
                BoneTransform(
                    rotation = addVectors(result.rotation, transform.rotation),
                    position = addVectors(result.position, transform.position),
                    scale = multiplyVectors(result.scale, transform.scale)
                )
            }
        }

        return result
    }

    /**
     * 获取所有有动画的骨骼名称
     */
    fun getAnimatedBoneNames(): Set<String> {
        val names = mutableSetOf<String>()
        players.values.forEach { player ->
            names.addAll(player.getAnimatedBoneNames())
        }
        return names
    }

    /**
     * 播放指定别名的动画
     */
    fun play(alias: String) {
        val animId = animationMap[alias] ?: return
        val anim = animations[animId] ?: return
        if (players.containsKey(alias)) {
            players[alias]?.reset()
        } else {
            players[alias] = AnimationPlayer(anim)
        }
    }

    /**
     * 停止指定别名的动画
     */
    fun stop(alias: String) {
        players.remove(alias)
    }

    /**
     * 重置所有动画
     */
    fun resetAll() {
        players.values.forEach { it.reset() }
    }

    /**
     * 检查是否有正在播放的动画
     */
    fun hasActiveAnimations(): Boolean {
        return players.values.any { it.isPlaying() }
    }

    /**
     * 向量加法
     */
    private fun addVectors(a: List<Double>?, b: List<Double>?): List<Double>? {
        if (a == null) return b
        if (b == null) return a
        val maxLen = maxOf(a.size, b.size)
        val aExtended = a + List(maxLen - a.size) { 0.0 }
        val bExtended = b + List(maxLen - b.size) { 0.0 }
        return aExtended.zip(bExtended) { av, bv -> av + bv }
    }

    /**
     * 向量乘法（用于缩放叠加）
     */
    private fun multiplyVectors(a: List<Double>?, b: List<Double>?): List<Double>? {
        if (a == null) return b
        if (b == null) return a
        val maxLen = maxOf(a.size, b.size)
        val aExtended = a + List(maxLen - a.size) { 1.0 }  // 缩放默认1.0
        val bExtended = b + List(maxLen - b.size) { 1.0 }
        return aExtended.zip(bExtended) { av, bv -> av * bv }
    }

    companion object {
        /**
         * 从 ClientEntity 描述创建动画管理器
         *
         * @param clientEntity 实体客户端定义
         * @param allAnimations 所有可用的动画（动画ID -> 动画数据）
         * @return 动画管理器，如果没有动画配置则返回 null
         */
        fun fromClientEntity(
            clientEntity: EntityResourceDefinition.ClientEntityDescription,
            allAnimations: Map<String, AnimationDefinition.Animation>
        ): EntityAnimationManager? {
            val animationMap = clientEntity.animations ?: return null
            val animateList = parseAnimateList(clientEntity.scripts?.animate)
            if (animateList.isEmpty()) return null

            return EntityAnimationManager(animationMap, allAnimations, animateList)
        }

        /**
         * 解析 scripts.animate 列表
         *
         * scripts.animate 支持两种格式：
         * 1. 字符串: "idle" - 无条件播放
         * 2. 对象: {"walk": "query.is_moving"} - 条件播放（当前忽略条件）
         */
        private fun parseAnimateList(animate: List<Any>?): List<String> {
            return animate?.mapNotNull { item ->
                when (item) {
                    is String -> item
                    is Map<*, *> -> {
                        // 从 Map 中取第一个键作为动画别名
                        // 条件表达式（值）当前被忽略
                        item.keys.firstOrNull()?.toString()
                    }
                    else -> null
                }
            } ?: emptyList()
        }
    }
}
