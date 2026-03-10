package net.easecation.bedrockloader.multiblock

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.definition.MultiblockDefinition
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.withIfExists
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.minecraft.block.Blocks
import net.minecraft.entity.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * 多方块结构的组装/拆解工具对象。
 * 所有方法均为纯函数（无副作用的外部依赖），调用方负责提供正确的参数。
 */
object MultiblockAssembler {

    /**
     * Y 轴旋转偏移量矩阵：将 (dx, dz) 按控制方块面向旋转。
     * 基准方向为 North；其他方向的顺序为 North→East→South→West（顺时针）。
     */
    fun rotateOffset(dx: Int, dz: Int, facing: Direction): Pair<Int, Int> {
        return when (facing) {
            Direction.NORTH -> Pair(dx, dz)
            Direction.EAST  -> Pair(-dz, dx)
            Direction.SOUTH -> Pair(-dx, -dz)
            Direction.WEST  -> Pair(dz, -dx)
            else            -> Pair(dx, dz)  // 垂直方向不支持旋转，fallback 无旋转
        }
    }

    /**
     * 将方向值按与 North 相同的旋转量旋转（用于 rotate_facing 的部件朝向计算）。
     * North 为基准 (0°)，顺时针旋转。
     */
    fun rotateDirection(dir: Direction, facing: Direction): Direction {
        if (dir.axis.isVertical) return dir  // 垂直方向不参与水平旋转
        return when (facing) {
            Direction.NORTH -> dir
            Direction.EAST  -> dir.rotateYClockwise()
            Direction.SOUTH -> dir.opposite
            Direction.WEST  -> dir.rotateYCounterclockwise()
            else            -> dir
        }
    }

    /**
     * 检查是否所有部件位置均可放置（未被其他方块占用）。
     *
     * @return 每个非控制部件的 (Part 定义, 目标 BlockPos) 列表；若任意位置不可放置则返回 null
     */
    fun checkCanAssemble(
        world: ServerWorld,
        controllerPos: BlockPos,
        def: MultiblockDefinition.Multiblock,
        facing: Direction
    ): List<Pair<MultiblockDefinition.Part, BlockPos>>? {
        val result = mutableListOf<Pair<MultiblockDefinition.Part, BlockPos>>()
        for (part in def.parts) {
            if (part.isController) continue  // 控制方块已放置，跳过
            val (rdx, rdz) = rotateOffset(part.offset[0], part.offset[2], facing)
            val targetPos = controllerPos.add(rdx, part.offset[1], rdz)

            // 检查目标位置是否可放置
            val targetState = world.getBlockState(targetPos)
            if (!targetState.isReplaceable) return null

            // 检查目标区块是否已加载（防止跨未加载区块组装）
            if (!world.isChunkLoaded(targetPos)) return null

            result.add(Pair(part, targetPos))
        }
        return result
    }

    /**
     * 将所有部件放置到世界，并注册到持久化状态。
     * 采用两阶段提交：先全部 setBlockState，出现异常时回滚已放置的部件。
     *
     * 放置规则：
     * 1. 从部件方块的 defaultState 开始
     * 2. 应用 stateOverrides（固定 state 约束，不参与旋转）
     * 3. 若 rotation.enabled 且 part.rotateFacing：旋转 facing_property 对应的方向
     * 4. 若 rotation.enabled 但 !part.rotateFacing：设置与控制方块相同的朝向
     */
    fun assemble(
        world: ServerWorld,
        controllerPos: BlockPos,
        parts: List<Pair<MultiblockDefinition.Part, BlockPos>>,
        facing: Direction,
        def: MultiblockDefinition.Multiblock,
        persistentState: MultiblockPersistentState
    ) {
        val placed = mutableListOf<BlockPos>()
        try {
            for ((part, targetPos) in parts) {
                val partBlock = BedrockAddonsRegistry.blocks[part.blockId]
                    ?: throw IllegalStateException("多方块部件方块未注册: ${part.blockId}")

                // 从 defaultState 开始构建目标 state
                var targetState = partBlock.defaultState

                // 应用固定的 stateOverrides
                if (part.stateOverrides.isNotEmpty() && partBlock is BlockContext.BlockDataDriven) {
                    val blockCtx = partBlock.getBlockContext()
                    for ((propName, propValue) in part.stateOverrides) {
                        val prop = blockCtx.properties[propName] ?: continue
                        targetState = applyStringProperty(targetState, prop, propValue)
                    }
                }

                // 应用旋转方向属性（若 rotation.enabled）
                val facingPropName = def.rotation?.takeIf { it.enabled }?.facingProperty
                if (facingPropName != null && partBlock is BlockContext.BlockDataDriven) {
                    val blockCtx = partBlock.getBlockContext()
                    val facingProp = blockCtx.properties[facingPropName]
                    if (facingProp != null && targetState.contains(facingProp.javaProperty)) {
                        val targetFacing = if (part.rotateFacing) {
                            // rotate_facing: 基准方向 + 控制方块旋转量 = 部件朝向
                            val baseDir = targetState.getOrEmpty(facingProp.javaProperty).orElse(null)
                            if (baseDir is Direction) rotateDirection(baseDir, facing) else facing
                        } else {
                            // 不额外旋转：与控制方块朝向相同
                            facing
                        }
                        @Suppress("UNCHECKED_CAST")
                        targetState = targetState.withIfExists(
                            facingProp.javaProperty as net.minecraft.state.property.Property<Direction>,
                            targetFacing
                        )
                    }
                }

                world.setBlockState(targetPos, targetState)
                placed.add(targetPos)
            }
        } catch (e: Exception) {
            // 出现异常时回滚已放置的部件
            placed.forEach { world.setBlockState(it, Blocks.AIR.defaultState) }
            BedrockLoader.logger.error("多方块组装失败（${def.identifier}），已回滚 ${placed.size} 个部件", e)
            throw e
        }

        // 注册到持久化状态
        persistentState.registerAssembly(controllerPos, def.identifier, placed)
    }

    /**
     * 拆解多方块结构：移除所有部件和控制方块，在控制方块位置掉落控制方块 item。
     *
     * @param world 服务器世界
     * @param controllerPos 控制方块位置
     * @param def 多方块定义
     * @param persistentState 持久化状态
     */
    fun disassemble(
        world: ServerWorld,
        controllerPos: BlockPos,
        def: MultiblockDefinition.Multiblock,
        persistentState: MultiblockPersistentState,
        dropItems: Boolean = true
    ) {
        // 注销并获取所有部件位置
        val partPositions = persistentState.unregisterAssembly(controllerPos)

        // 移除所有部件
        partPositions.forEach { pos ->
            if (!world.getBlockState(pos).isAir) {
                world.setBlockState(pos, Blocks.AIR.defaultState)
            }
        }

        // 移除控制方块
        if (!world.getBlockState(controllerPos).isAir) {
            world.setBlockState(controllerPos, Blocks.AIR.defaultState)
        }

        // 在控制方块位置掉落控制方块 item（创意模式不掉落）
        if (dropItems) {
            val controllerPart = def.parts.find { it.isController } ?: return
            val ctrlItem = BedrockAddonsRegistry.items[controllerPart.blockId] ?: return
            world.spawnEntity(ItemEntity(
                world,
                controllerPos.x + 0.5,
                controllerPos.y + 0.5,
                controllerPos.z + 0.5,
                ItemStack(ctrlItem)
            ))
        }
    }

    /**
     * 拆解多方块结构，跳过指定位置（用于爆炸场景：该位置已被移除）。
     *
     * @param skipPos 已被外部移除的位置（不再重复 setBlockState AIR）
     */
    fun disassembleSkipping(
        world: ServerWorld,
        controllerPos: BlockPos,
        def: MultiblockDefinition.Multiblock,
        persistentState: MultiblockPersistentState,
        skipPos: BlockPos,
        dropItems: Boolean = true
    ) {
        val partPositions = persistentState.unregisterAssembly(controllerPos)

        partPositions.filter { it != skipPos }.forEach { pos ->
            if (!world.getBlockState(pos).isAir) {
                world.setBlockState(pos, Blocks.AIR.defaultState)
            }
        }

        if (controllerPos != skipPos && !world.getBlockState(controllerPos).isAir) {
            world.setBlockState(controllerPos, Blocks.AIR.defaultState)
        }

        if (dropItems) {
            val controllerPart = def.parts.find { it.isController } ?: return
            val ctrlItem = BedrockAddonsRegistry.items[controllerPart.blockId] ?: return
            world.spawnEntity(ItemEntity(
                world,
                controllerPos.x + 0.5,
                controllerPos.y + 0.5,
                controllerPos.z + 0.5,
                ItemStack(ctrlItem)
            ))
        }
    }

    /**
     * 通过字符串值为 BedrockProperty 设置方块状态（类型安全桥接）。
     * 遍历属性所有合法值，找到 toString() 匹配的值后设置。
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyStringProperty(
        state: net.minecraft.block.BlockState,
        prop: net.easecation.bedrockloader.block.property.BedrockProperty<*>,
        value: String
    ): net.minecraft.block.BlockState {
        val javaProp = prop.javaProperty as net.minecraft.state.property.Property<Comparable<Any>>
        val matchedValue = javaProp.values.find { it.toString() == value } ?: return state
        return if (state.contains(javaProp)) state.with(javaProp, matchedValue) else state
    }
}
