package net.easecation.bedrockloader.block.condition

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.block.property.BedrockProperty
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier

/**
 * 预编译的Molang条件表达式
 *
 * 将基岩版的Molang条件字符串预编译为可执行的Java表示，
 * 避免运行时重复解析字符串，提升性能。
 *
 * 支持的基岩版原生语法：
 * - query.block_state('property') == 'value'
 * - query.block_neighbor_has_all_tags(x, y, z, 'tag1', 'tag2', ...)
 * - query.block_neighbor_has_any_tag(x, y, z, 'tag1', 'tag2', ...)
 * - 逻辑运算：&&, ||, !
 */
sealed class CompiledCondition {

    /**
     * 检查条件是否为静态条件（仅依赖BlockState本身，不需要World上下文）
     *
     * 静态条件可以在初始化时预计算，无需运行时求值。
     * 动态条件需要访问World（如邻居查询），必须在运行时求值。
     *
     * @return true=静态条件（可预计算），false=动态条件（需运行时求值）
     */
    abstract fun isStatic(): Boolean

    /**
     * 求值方法，在运行时快速判断条件是否满足
     *
     * @param context 包含世界、位置、状态的上下文
     * @param properties 方块的Bedrock属性映射（用于block_state查询）
     * @return 条件是否满足
     */
    abstract fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean

    /**
     * 方块自身状态查询
     *
     * 对应：query.block_state('property') == 'value'
     *
     * @property propertyName 属性名称
     * @property operator 比较运算符（"==" 或 "!="）
     * @property expectedValue 期望值
     */
    data class BlockStateCheck(
        val propertyName: String,
        val operator: String,
        val expectedValue: String
    ) : CompiledCondition() {

        override fun isStatic(): Boolean = true  // 仅依赖BlockState本身，无需World

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            val property = properties[propertyName]
            if (property == null) {
                BedrockLoader.logger.warn("[CompiledCondition] Unknown property '$propertyName' in block_state check")
                return false
            }

            val actualValue = property.getBedrockValueName(context.state)
            if (actualValue == null) {
                BedrockLoader.logger.warn("[CompiledCondition] Property '$propertyName' not present in state")
                return false
            }

            return when (operator) {
                "==" -> actualValue == expectedValue
                "!=" -> actualValue != expectedValue
                else -> {
                    BedrockLoader.logger.warn("[CompiledCondition] Unsupported operator '$operator'")
                    false
                }
            }
        }
    }

    /**
     * 邻居方块Tag查询
     *
     * 对应：query.block_neighbor_has_all_tags(x, y, z, 'tag1', 'tag2', ...)
     *      query.block_neighbor_has_any_tag(x, y, z, 'tag1', 'tag2', ...)
     *
     * @property offsetX X轴偏移（相对于当前方块）
     * @property offsetY Y轴偏移
     * @property offsetZ Z轴偏移
     * @property tags 要检查的Tag名称列表
     * @property matchAll true=所有tag都匹配（all_tags），false=至少一个匹配（any_tag）
     */
    data class NeighborTagCheck(
        val offsetX: Int,
        val offsetY: Int,
        val offsetZ: Int,
        val tags: List<String>,
        val matchAll: Boolean
    ) : CompiledCondition() {

        override fun isStatic(): Boolean = false  // 需要World上下文查询邻居方块

        // 延迟初始化TagKey，避免静态初始化问题
        private val tagKeys: List<TagKey<net.minecraft.block.Block>> by lazy {
            tags.map { tagName ->
                // 规范化tag名称
                val identifier = when {
                    tagName.contains(':') -> {
                        val parts = tagName.split(':', limit = 2)
                        Identifier.of(parts[0], parts[1])
                    }
                    else -> Identifier.of("minecraft", tagName)
                }
                TagKey.of(RegistryKeys.BLOCK, identifier)
            }
        }

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            // 邻居查询需要World上下文
            if (context.world == null) {
                BedrockLoader.logger.error("[CompiledCondition] NeighborTagCheck requires World context but got null")
                return false
            }

            val neighborPos = context.pos.add(offsetX, offsetY, offsetZ)
            val neighborState = context.world.getBlockState(neighborPos)

            return if (matchAll) {
                tagKeys.all { tag -> neighborState.isIn(tag) }
            } else {
                tagKeys.any { tag -> neighborState.isIn(tag) }
            }
        }
    }

    /**
     * 逻辑与运算
     *
     * 对应：condition1 && condition2
     */
    data class And(
        val left: CompiledCondition,
        val right: CompiledCondition
    ) : CompiledCondition() {

        override fun isStatic(): Boolean = left.isStatic() && right.isStatic()  // 两个都静态才是静态

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            // 短路求值
            return left.evaluate(context, properties) && right.evaluate(context, properties)
        }
    }

    /**
     * 逻辑或运算
     *
     * 对应：condition1 || condition2
     */
    data class Or(
        val left: CompiledCondition,
        val right: CompiledCondition
    ) : CompiledCondition() {

        override fun isStatic(): Boolean = left.isStatic() && right.isStatic()  // 两个都静态才是静态

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            // 短路求值
            return left.evaluate(context, properties) || right.evaluate(context, properties)
        }
    }

    /**
     * 逻辑非运算
     *
     * 对应：!condition
     */
    data class Not(
        val inner: CompiledCondition
    ) : CompiledCondition() {

        override fun isStatic(): Boolean = inner.isStatic()  // 继承内部条件的静态性

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            return !inner.evaluate(context, properties)
        }
    }

    /**
     * 恒真条件（用于测试或默认情况）
     */
    object AlwaysTrue : CompiledCondition() {

        override fun isStatic(): Boolean = true  // 常量是静态的

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            return true
        }
    }

    /**
     * 恒假条件（用于测试或默认情况）
     */
    object AlwaysFalse : CompiledCondition() {

        override fun isStatic(): Boolean = true  // 常量是静态的

        override fun evaluate(context: ConditionContext, properties: Map<String, BedrockProperty<*, *>>): Boolean {
            return false
        }
    }
}
