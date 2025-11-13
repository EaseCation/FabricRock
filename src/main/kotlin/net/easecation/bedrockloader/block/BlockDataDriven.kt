package net.easecation.bedrockloader.block

import com.mojang.serialization.MapCodec
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.ComponentSelectionBox
import net.easecation.bedrockloader.bedrock.block.component.FaceDirectionalType
import net.easecation.bedrockloader.bedrock.block.state.StateBoolean
import net.easecation.bedrockloader.bedrock.block.state.StateInt
import net.easecation.bedrockloader.bedrock.block.state.StateRange
import net.easecation.bedrockloader.bedrock.block.state.StateString
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementDirection
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementPosition
import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.easecation.bedrockloader.block.condition.CompiledCondition
import net.easecation.bedrockloader.block.condition.ConditionContext
import net.easecation.bedrockloader.block.condition.MolangConditionCompiler
import net.easecation.bedrockloader.block.property.*
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.minecraft.block.*
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.enums.BlockHalf
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf

data class BlockContext(
    val identifier: Identifier,
    val behaviour: BlockBehaviourDefinition.BlockBehaviour,
    val properties: Map<String, BedrockProperty<*, *>>,
) {
    companion object {
        // minecraft:placement_direction
        val MINECRAFT_CARDINAL_DIRECTION = BedrockDirectionProperty.of("minecraft:cardinal_direction", Direction.Type.HORIZONTAL.toSet())
        val MINECRAFT_FACING_DIRECTION = BedrockDirectionProperty.of("minecraft:facing_direction")
        // minecraft:placement_position
        val MINECRAFT_BLOCK_FACE = BedrockDirectionProperty.of("minecraft:block_face")
        val MINECRAFT_VERTICAL_HALF = BedrockEnumProperty.of<BlockHalf>("minecraft:vertical_half")
        // netease:face_directional
        val DIRECTION = BedrockIntProperty.of("direction", (0..3).toSet())
        val FACING_DIRECTION = BedrockIntProperty.of("facing_direction", (0..5).toSet())

        /**
         * 创建PERMUTATION_STATE属性
         *
         * 用于存储当前激活的permutation索引：
         * - 0: 无permutation匹配（使用基础components）
         * - 1+: 第N个permutation匹配
         *
         * @param permutationCount permutations列表的大小
         * @return Bedrock整数属性
         */
        fun createPermutationStateProperty(permutationCount: Int): BedrockIntProperty {
            return BedrockIntProperty.of(
                "bedrock:permutation_state",
                (0..permutationCount).toSet()
            )
        }

        fun create(identifier: Identifier, behaviour: BlockBehaviourDefinition.BlockBehaviour): BlockDataDriven {
            val components = behaviour.components

            fun calculateProperties(): Map<String, BedrockProperty<*, *>> {
                val placementDirection: Map<String, BedrockProperty<*, *>> = behaviour.description.traits?.minecraftPlacementDirection?.enabled_states?.map { state ->
                    when (state) {
                        TraitPlacementDirection.State.MINECRAFT_CARDINAL_DIRECTION -> MINECRAFT_CARDINAL_DIRECTION
                        TraitPlacementDirection.State.MINECRAFT_FACING_DIRECTION -> MINECRAFT_FACING_DIRECTION
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()
                val placementPosition: Map<String, BedrockProperty<*, *>> = behaviour.description.traits?.minecraftPlacementPosition?.enabled_states?.map { state ->
                    when (state) {
                        TraitPlacementPosition.State.MINECRAFT_BLOCK_FACE -> MINECRAFT_BLOCK_FACE
                        TraitPlacementPosition.State.MINECRAFT_VERTICAL_HALF -> MINECRAFT_VERTICAL_HALF
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()
                val faceDirectional: Map<String, BedrockProperty<*, *>> = behaviour.components.neteaseFaceDirectional?.type?.let { type ->
                    mapOf(type.name to when (type) {
                        FaceDirectionalType.direction -> DIRECTION
                        FaceDirectionalType.facing_direction -> FACING_DIRECTION
                    })
                } ?: emptyMap()
                val properties: Map<String, BedrockProperty<*, *>> = behaviour.description.states?.mapValues { (key, state) ->
                    when (state) {
                        is StateBoolean -> BedrockBooleanProperty.of(key, state.toSet())
                        is StateInt -> BedrockIntProperty.of(key, state.toSet())
                        is StateString -> BedrockStringProperty.of(key, state.toSet())
                        is StateRange -> BedrockIntProperty.of(key, (state.values.min..state.values.max).toSet())
                    }
                } ?: emptyMap()

                // 仅在有动态permutations时添加permutation_state属性
                // 动态permutations是指需要World上下文的条件（如邻居查询）
                val permutationState: Map<String, BedrockProperty<*, *>> = if (behaviour.permutations != null && behaviour.permutations!!.isNotEmpty()) {
                    // 预编译所有permutations以检测是否有动态条件
                    val hasDynamicPermutations = behaviour.permutations!!.any { (condition, _) ->
                        try {
                            val compiled = MolangConditionCompiler.compile(condition)
                            !compiled.isStatic()  // 检查是否为动态条件
                        } catch (e: Exception) {
                            false  // 编译失败视为静态
                        }
                    }

                    if (hasDynamicPermutations) {
                        // 计算动态permutations的数量
                        val dynamicCount = behaviour.permutations!!.count { (condition, _) ->
                            try {
                                val compiled = MolangConditionCompiler.compile(condition)
                                !compiled.isStatic()
                            } catch (e: Exception) {
                                false
                            }
                        }
                        val permutationProperty = createPermutationStateProperty(dynamicCount)
                        mapOf(permutationProperty.getBedrockName() to permutationProperty)
                    } else {
                        emptyMap()  // 仅有静态permutations，无需额外属性
                    }
                } else {
                    emptyMap()
                }

                return placementDirection + placementPosition + faceDirectional + properties + permutationState
            }

            fun calculateSettings(): Settings {
                val settings = Settings.create().hardness(4.0f).nonOpaque()  // TODO hardness
                components.minecraftCollisionBox?.let {
                    when (it) {
                        is ComponentCollisionBox.ComponentCollisionBoxBoolean -> {
                            if (!it.value) {
                                settings.noCollision()
                            }
                        }

                        is ComponentCollisionBox.ComponentCollisionBoxCustom -> {
                            if (it.size.all { e -> e == 0f }) {
                                settings.noCollision()
                            }
                        }
                    }

                }
                components.minecraftLightEmission?.let {
                    settings.luminance { _ -> it }
                }
                return settings
            }

            val settings = calculateSettings()
            val properties = calculateProperties()
            return BlockContext(identifier, behaviour, properties).BlockDataDriven(settings)
        }
    }

    inner class BlockDataDriven(settings: Settings) : BlockWithEntity(settings) {

        // ========== Permutations系统 ==========

        /**
         * 预编译的permutation条件列表
         * 每个元素为 (编译后的条件, 对应的components) 对
         */
        private val compiledPermutations: List<Pair<CompiledCondition, BlockComponents>>

        /**
         * 静态permutations（仅依赖BlockState本身，可在初始化时预计算）
         */
        private val staticPermutations: List<Pair<CompiledCondition, BlockComponents>>

        /**
         * 动态permutations（依赖World上下文，必须运行时求值）
         */
        private val dynamicPermutations: List<Pair<CompiledCondition, BlockComponents>>

        /**
         * 获取permutation_state属性（仅在有动态permutations时存在）
         */
        private val permutationStateProperty: BedrockIntProperty? =
            properties["bedrock:permutation_state"] as? BedrockIntProperty

        /**
         * 每个完整BlockState对应的components缓存
         */
        private val componentsByState: Map<BlockState, BlockComponents>

        init {
            // 1. 预编译所有permutations
            compiledPermutations = if (behaviour.permutations != null) {
                behaviour.permutations!!.mapNotNull { (condition, components) ->
                    try {
                        val compiled = MolangConditionCompiler.compile(condition)
                        compiled to components
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("[BlockDataDriven] Failed to compile permutation condition for block $identifier: $condition", e)
                        null  // 跳过编译失败的permutation
                    }
                }
            } else {
                emptyList()
            }

            // 2. 分类为静态和动态permutations
            val (static, dynamic) = compiledPermutations.partition { (condition, _) ->
                condition.isStatic()
            }
            staticPermutations = static
            dynamicPermutations = dynamic

            if (compiledPermutations.isNotEmpty()) {
                BedrockLoader.logger.info("[BlockDataDriven] Block $identifier: ${staticPermutations.size} static + ${dynamicPermutations.size} dynamic permutations")
            }

            // 3. 设置默认状态
            defaultState = stateManager.defaultState
                .withIfExists(MINECRAFT_CARDINAL_DIRECTION, Direction.SOUTH)
                .withIfExists(MINECRAFT_FACING_DIRECTION, Direction.DOWN)
                .withIfExists(MINECRAFT_BLOCK_FACE, Direction.DOWN)
                .withIfExists(MINECRAFT_VERTICAL_HALF, BlockHalf.BOTTOM)
                .withIfExists(DIRECTION, 0)
                .withIfExists(FACING_DIRECTION, 0)
                .withIfExists(permutationStateProperty, 0)  // 默认无动态permutation匹配

            // 4. 为每个BlockState预烘焙components（混合策略）
            componentsByState = stateManager.states.associateWith { state ->
                // 步骤1：应用所有静态permutations（初始化时求值）
                var components = behaviour.components
                staticPermutations.forEach { (condition, permComponents) ->
                    try {
                        // 对于静态条件，可以在初始化时求值（不需要World）
                        if (condition.evaluate(ConditionContext(null, BlockPos.ORIGIN, state), properties)) {
                            components = components.mergeComponents(permComponents)
                        }
                    } catch (e: Exception) {
                        BedrockLoader.logger.warn("[BlockDataDriven] Static permutation evaluation failed for block $identifier", e)
                    }
                }

                // 步骤2：如果有动态permutations，根据permutation_state继续合并
                if (permutationStateProperty != null && dynamicPermutations.isNotEmpty()) {
                    val permutationIndex = state[permutationStateProperty]
                    if (permutationIndex > 0 && permutationIndex <= dynamicPermutations.size) {
                        val (_, dynComponents) = dynamicPermutations[permutationIndex - 1]
                        components = components.mergeComponents(dynComponents)
                    }
                }

                components
            }

            BedrockLoader.logger.debug("[BlockDataDriven] Block $identifier has ${stateManager.states.size} total states")
        }

        /**
         * 运行时求值动态permutations，找到匹配的permutation索引
         *
         * 仅遍历动态permutations（需要World上下文的条件），找到第一个满足的条件。
         * 静态permutations已经在初始化时预计算，不在此处理。
         *
         * @param world 世界访问器
         * @param pos 方块位置
         * @param state 当前方块状态
         * @return 动态permutation索引（0表示无匹配，1+表示第N个动态permutation）
         */
        private fun evaluatePermutations(world: WorldAccess, pos: BlockPos, state: BlockState): Int {
            if (dynamicPermutations.isEmpty()) {
                return 0
            }

            val context = ConditionContext(world, pos, state)

            // 遍历动态permutations，找到第一个匹配的
            dynamicPermutations.forEachIndexed { index, (condition, _) ->
                try {
                    if (condition.evaluate(context, properties)) {
                        return index + 1
                    }
                } catch (e: Exception) {
                    BedrockLoader.logger.error("[BlockDataDriven] Error evaluating dynamic permutation $index for block $identifier", e)
                }
            }

            return 0  // 无匹配
        }

        fun applyTransformation(state: BlockState, box: Box): Box {
            val transformation = getComponents(state).minecraftTransformation ?: return box
            return transformation.apply(box)
        }

        fun getFaceQuaternion(direction: Direction): Quaternionf {
            return when (direction) {
                Direction.DOWN -> Quaternionf().rotationX((-Math.PI / 2).toFloat())
                Direction.UP -> Quaternionf().rotationX((Math.PI / 2).toFloat())
                Direction.NORTH -> Quaternionf()
                Direction.SOUTH -> Quaternionf().rotationY(Math.PI.toFloat())
                Direction.WEST -> Quaternionf().rotationY((Math.PI / 2).toFloat())
                Direction.EAST -> Quaternionf().rotationY((-Math.PI / 2).toFloat())
            }
        }

        fun applyFaceDirectional(state: BlockState, position: Matrix4f, normal: Matrix3f) {
            val faceDirectional = getComponents(state).neteaseFaceDirectional ?: return
            val direction = when (faceDirectional.type) {
                FaceDirectionalType.direction -> Direction.fromHorizontal(state[DIRECTION])
                FaceDirectionalType.facing_direction -> Direction.byId(state[FACING_DIRECTION])
            }
            val faceQuaternion = getFaceQuaternion(direction)
            position.rotateAround(faceQuaternion, 0.5f, 0.5f, 0.5f)
            normal.rotate(faceQuaternion)
        }

        fun getComponents(state: BlockState): BlockComponents {
            return componentsByState[state] ?: behaviour.components
        }

        /**
         * 获取特定BlockState的几何体标识符
         *
         * 用于支持permutations中的geometry动态切换。
         * 从当前state的components中提取minecraft:geometry组件的标识符。
         *
         * @param state 方块状态
         * @return geometry标识符，如果没有定义geometry则返回null
         */
        fun getGeometryIdentifier(state: BlockState): String? {
            return when (val geometry = getComponents(state).minecraftGeometry) {
                is net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                is net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                null -> null
            }
        }

        override fun getCodec(): MapCodec<out BlockWithEntity> {
            return createCodec(::BlockDataDriven)
        }

        override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
            return BedrockAddonsRegistry.blockEntities[identifier]?.let { return it.instantiate(pos, state) }
        }

        override fun getRenderType(state: BlockState?): BlockRenderType {
            if (BedrockAddonsRegistry.blockEntities[identifier] != null) {
                return BlockRenderType.INVISIBLE
            } else {
                return BlockRenderType.MODEL
            }
        }

        private fun rotateDirection(direction: Direction, yRotationOffset: Int): Direction {
            if (direction.axis.isVertical) return direction
            val offset = (yRotationOffset / 90 % 4).let { if (it < 0) it + 4 else it }
            return when (offset) {
                1 -> direction.rotateYClockwise()
                2 -> direction.opposite
                3 -> direction.rotateYCounterclockwise()
                else -> direction
            }
        }
        override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
            val yRotationOffset =  behaviour.description.traits?.minecraftPlacementDirection?.y_rotation_offset ?: 0
            var state = defaultState
                .withIfExists(MINECRAFT_CARDINAL_DIRECTION, rotateDirection(ctx.horizontalPlayerFacing, yRotationOffset))
                .withIfExists(MINECRAFT_FACING_DIRECTION, rotateDirection(ctx.playerLookDirection, yRotationOffset))
                .withIfExists(MINECRAFT_BLOCK_FACE, ctx.side)
                .withIfExists(
                    MINECRAFT_VERTICAL_HALF, when {
                        ctx.side == Direction.DOWN -> BlockHalf.TOP
                        ctx.side == Direction.UP -> BlockHalf.BOTTOM
                        ctx.hitPos.y - ctx.blockPos.y > 0.5 -> BlockHalf.TOP
                        else -> BlockHalf.BOTTOM
                    }
                )
                .withIfExists(DIRECTION, ctx.horizontalPlayerFacing.opposite.horizontal)
                .withIfExists(FACING_DIRECTION, ctx.playerLookDirection.opposite.id)

            // 求值permutations并设置permutation_state
            if (permutationStateProperty != null) {
                val permutationIndex = evaluatePermutations(ctx.world, ctx.blockPos, state)
                state = state.with(permutationStateProperty, permutationIndex)
            }

            return state
        }

        override fun getStateForNeighborUpdate(
            state: BlockState,
            direction: Direction,
            neighborState: BlockState,
            world: WorldAccess,
            pos: BlockPos,
            neighborPos: BlockPos
        ): BlockState {
            // 如果没有permutations，直接返回原状态
            if (permutationStateProperty == null) {
                return state
            }

            // 重新求值permutations（邻居变化可能影响条件结果）
            val newPermutationIndex = evaluatePermutations(world, pos, state)
            val currentPermutationIndex = state[permutationStateProperty]

            // 仅当permutation变化时才更新BlockState
            return if (newPermutationIndex != currentPermutationIndex) {
                state.with(permutationStateProperty, newPermutationIndex)
            } else {
                state
            }
        }

        override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
            properties.values.forEach { builder.add(it.javaProperty) }
        }

        override fun getCollisionShape(
            state: BlockState,
            world: BlockView,
            pos: BlockPos,
            context: ShapeContext
        ): VoxelShape = when {
            this.collidable -> when (val box = getComponents(state).minecraftCollisionBox) {
                is ComponentCollisionBox.ComponentCollisionBoxBoolean -> getOutlineShape(state, world, pos, context)
                is ComponentCollisionBox.ComponentCollisionBoxCustom -> VoxelShapes.cuboid(applyTransformation(state, Box(
                    (1.0 / 16) * (16 - (box.origin[0] + 8 + box.size[0])),
                    (1.0 / 16) * (box.origin[1]),
                    (1.0 / 16) * (box.origin[2] + 8),
                    (1.0 / 16) * (16 - (box.origin[0] + 8)),
                    (1.0 / 16) * (box.origin[1] + box.size[1]),
                    (1.0 / 16) * (box.origin[2] + 8 + box.size[2])
                )))
                else -> getOutlineShape(state, world, pos, context)
            }
            else -> VoxelShapes.empty()
        }

        override fun getOutlineShape(
            state: BlockState,
            world: BlockView,
            pos: BlockPos,
            context: ShapeContext
        ): VoxelShape = when (val box = getComponents(state).minecraftSelectionBox) {
            is ComponentSelectionBox.ComponentSelectionBoxBoolean -> super.getOutlineShape(state, world, pos, context)
            is ComponentSelectionBox.ComponentSelectionBoxCustom -> {
                VoxelShapes.cuboid(applyTransformation(state, Box(
                    (1.0 / 16) * (16 - (box.origin[0] + 8 + box.size[0])),
                    (1.0 / 16) * (box.origin[1]),
                    (1.0 / 16) * (box.origin[2] + 8),
                    (1.0 / 16) * (16 - (box.origin[0] + 8)),
                    (1.0 / 16) * (box.origin[1] + box.size[1]),
                    (1.0 / 16) * (box.origin[2] + 8 + box.size[2])
                )))
            }
            else -> super.getOutlineShape(state, world, pos, context)
        }
    }
}