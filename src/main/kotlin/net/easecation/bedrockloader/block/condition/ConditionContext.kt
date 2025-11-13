package net.easecation.bedrockloader.block.condition

import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess

/**
 * Molang条件求值的上下文信息
 *
 * 用于在运行时提供方块状态查询所需的所有数据，
 * 包括世界访问器、方块位置和当前方块状态。
 *
 * @property world 世界访问器，用于查询邻居方块（静态条件可为null）
 * @property pos 当前方块位置
 * @property state 当前方块状态
 */
data class ConditionContext(
    val world: WorldAccess?,
    val pos: BlockPos,
    val state: BlockState
)
