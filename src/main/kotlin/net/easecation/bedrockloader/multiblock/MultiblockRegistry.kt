package net.easecation.bedrockloader.multiblock

import net.easecation.bedrockloader.bedrock.definition.MultiblockDefinition
import net.easecation.bedrockloader.block.BlockContext
import net.minecraft.block.BlockState
import net.minecraft.util.Identifier

/**
 * 多方块结构的运行时注册表。
 *
 * 索引策略：
 * - byBlockId：某个方块 ID 参与了哪些多方块定义（controller 和 part 均计入），用于快速过滤无关方块
 * - byIdentifier：多方块 identifier → 定义，用于从持久化状态恢复时查找定义
 */
object MultiblockRegistry {

    /** blockId → 使用该方块的多方块定义列表 */
    val byBlockId: MutableMap<Identifier, MutableList<MultiblockDefinition.Multiblock>> = mutableMapOf()

    /** 多方块 identifier → 定义 */
    val byIdentifier: MutableMap<Identifier, MultiblockDefinition.Multiblock> = mutableMapOf()

    fun register(def: MultiblockDefinition.Multiblock) {
        byIdentifier[def.identifier] = def
        def.parts.forEach { part ->
            byBlockId.getOrPut(part.blockId) { mutableListOf() }.add(def)
        }
    }

    /**
     * 判断给定的 blockId + 当前 BlockState 是否匹配某个多方块定义的控制方块。
     * 同时检查 stateOverrides 是否全部满足（支持单 Block ID 模式中的不同 state 值区分）。
     *
     * @param blockId 当前方块 ID
     * @param blockContext 当前方块的 BlockContext（提供 properties 访问）
     * @param state 当前 BlockState
     * @return 匹配到的多方块定义，若无则返回 null
     */
    fun findControllerDef(
        blockId: Identifier,
        blockContext: BlockContext,
        state: BlockState
    ): MultiblockDefinition.Multiblock? {
        return byBlockId[blockId]?.firstOrNull { def ->
            val ctrlPart = def.parts.find { it.isController } ?: return@firstOrNull false
            if (ctrlPart.blockId != blockId) return@firstOrNull false
            // 检查所有 stateOverrides 是否与当前 state 匹配
            ctrlPart.stateOverrides.all { (propName, propValue) ->
                val prop = blockContext.properties[propName] ?: return@all false
                state.getOrEmpty(prop.javaProperty).map { it.toString() }.orElse(null) == propValue
            }
        }
    }
}
