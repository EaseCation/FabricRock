package net.easecation.bedrockloader.block

import com.mojang.serialization.MapCodec
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.bedrock.block.component.ComponentSelectionBox
import net.easecation.bedrockloader.bedrock.block.component.FaceDirectionalType
import net.easecation.bedrockloader.bedrock.block.state.StateBoolean
import net.easecation.bedrockloader.bedrock.block.state.StateInt
import net.easecation.bedrockloader.bedrock.block.state.StateRange
import net.easecation.bedrockloader.bedrock.block.state.StateString
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementDirection
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementPosition
import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
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
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d

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
                return placementDirection + placementPosition + faceDirectional + properties
            }

            fun calculateSettings(): Settings {
                val settings = Settings.create().hardness(4.0f)  // TODO hardness

                // 面剔除控制：根据是否有自定义 geometry、透明渲染方法或方块实体决定
                // Full block（无自定义 geometry）：默认启用面剔除，确保性能优化
                // 有自定义 geometry、透明渲染或方块实体：禁用面剔除
                val hasCustomGeometry = components.minecraftGeometry != null
                val hasBlockEntity = components.neteaseBlockEntity != null
                val renderMethod = components.minecraftMaterialInstances?.get("*")?.render_method
                val needsNonOpaque = hasCustomGeometry ||
                    hasBlockEntity ||
                    renderMethod == ComponentMaterialInstances.RenderMethod.blend ||
                    renderMethod == ComponentMaterialInstances.RenderMethod.alpha_test

                if (needsNonOpaque) {
                    settings.nonOpaque()
                }

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

        private val conditionSplitRegex = """\s*&&\s*""".toRegex()
        private val blockStateRegex = """^\s*q(uery)?\s*\.\s*block_state\s*\(\s*'(?<key>[^']+)'\s*\)\s*(?<operator>==|!=)\s*'(?<value>[^']+)'\s*$""".toRegex()

        private val componentsByState: Map<BlockState, BlockComponents>

        init {
            defaultState = stateManager.defaultState
                .withIfExists(MINECRAFT_CARDINAL_DIRECTION, Direction.SOUTH)
                .withIfExists(MINECRAFT_FACING_DIRECTION, Direction.DOWN)
                .withIfExists(MINECRAFT_BLOCK_FACE, Direction.DOWN)
                .withIfExists(MINECRAFT_VERTICAL_HALF, BlockHalf.BOTTOM)
                .withIfExists(DIRECTION, 0)
                .withIfExists(FACING_DIRECTION, 0)
            componentsByState = stateManager.states.associateWith { bakeComponents(it) }
        }

        private fun bakeComponents(state: BlockState): BlockComponents {
            var activated = behaviour.components
            behaviour.permutations?.forEach { (condition, components) ->
                val conditions = conditionSplitRegex.split(condition)
                val satisfied = conditions.all { evalBlockStateCondition(it, state) }
                if (satisfied) {
                    activated = activated.mergeComponents(components)
                }
            }
            return activated
        }

        private fun evalBlockStateCondition(condition: String, state: BlockState): Boolean {
            val matchResult = blockStateRegex.matchEntire(condition)
            if (matchResult == null) {
                BedrockLoader.logger.warn("[BlockDataDriven] Block $identifier contains unsupported permutation block state condition: $condition")
                return false
            }
            val key = matchResult.groups["key"]?.value ?: return false
            val operator = matchResult.groups["operator"]?.value ?: return false
            val value = matchResult.groups["value"]?.value ?: return false
            val property = properties[key]
            if (property == null) {
                BedrockLoader.logger.warn("[BlockDataDriven] Block $identifier contains unknown property in permutation: $key")
                return false
            }
            val valueName = property.getBedrockValueName(state) ?: return false
            return when (operator) {
                "==" -> valueName == value
                "!=" -> valueName != value
                else -> false
            }
        }

        fun applyTransformation(state: BlockState, box: Box): Box {
            val transformation = getComponents(state).minecraftTransformation ?: return box
            return transformation.apply(box)
        }

        fun applyFaceDirectionalToBox(state: BlockState, box: Box): Box {
            val faceDirectional = getComponents(state).neteaseFaceDirectional ?: return box
            val direction = when (faceDirectional.type) {
                FaceDirectionalType.direction -> Direction.fromHorizontal(state[DIRECTION])
                FaceDirectionalType.facing_direction -> Direction.byId(state[FACING_DIRECTION])
            }
            val quaternion = getFaceQuaternion(direction)
            // 使用四元数围绕方块中心 (0.5, 0.5, 0.5) 旋转 Box
            val matrix = Matrix4d().rotateAround(
                Quaterniond(quaternion.x.toDouble(), quaternion.y.toDouble(), quaternion.z.toDouble(), quaternion.w.toDouble()),
                0.5, 0.5, 0.5
            )
            val p1 = Vector3d(box.minX, box.minY, box.minZ).mulProject(matrix)
            val p2 = Vector3d(box.maxX, box.maxY, box.maxZ).mulProject(matrix)
            // 重新计算 min/max（旋转后可能交换）
            return Box(
                minOf(p1.x, p2.x), minOf(p1.y, p2.y), minOf(p1.z, p2.z),
                maxOf(p1.x, p2.x), maxOf(p1.y, p2.y), maxOf(p1.z, p2.z)
            )
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
            return defaultState
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
                is ComponentCollisionBox.ComponentCollisionBoxCustom -> VoxelShapes.cuboid(
                    applyFaceDirectionalToBox(state, applyTransformation(state, Box(
                        (1.0 / 16) * (16 - (box.origin[0] + 8 + box.size[0])),
                        (1.0 / 16) * (box.origin[1]),
                        (1.0 / 16) * (box.origin[2] + 8),
                        (1.0 / 16) * (16 - (box.origin[0] + 8)),
                        (1.0 / 16) * (box.origin[1] + box.size[1]),
                        (1.0 / 16) * (box.origin[2] + 8 + box.size[2])
                    )))
                )
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
                VoxelShapes.cuboid(applyFaceDirectionalToBox(state, applyTransformation(state, Box(
                    (1.0 / 16) * (16 - (box.origin[0] + 8 + box.size[0])),
                    (1.0 / 16) * (box.origin[1]),
                    (1.0 / 16) * (box.origin[2] + 8),
                    (1.0 / 16) * (16 - (box.origin[0] + 8)),
                    (1.0 / 16) * (box.origin[1] + box.size[1]),
                    (1.0 / 16) * (box.origin[2] + 8 + box.size[2])
                ))))
            }
            else -> super.getOutlineShape(state, world, pos, context)
        }

        override fun getSidesShape(state: BlockState, world: BlockView, pos: BlockPos): VoxelShape {
            // 如果碰撞箱有效（非空），返回完整立方体以支持侧面放置物品（如悬挂木牌、火把等）
            return when (val box = getComponents(state).minecraftCollisionBox) {
                is ComponentCollisionBox.ComponentCollisionBoxBoolean ->
                    if (box.value) VoxelShapes.fullCube() else VoxelShapes.empty()
                is ComponentCollisionBox.ComponentCollisionBoxCustom ->
                    if (box.size.any { it > 0f }) VoxelShapes.fullCube() else VoxelShapes.empty()
                else -> VoxelShapes.fullCube()
            }
        }

        /**
         * 控制光照穿透（独立于面剔除）
         * 基岩版 minecraft:light_dampening 组件
         * 默认值：有方块实体时为 0（接收光照），否则为 15（完全阻挡）
         */
        @Deprecated("", ReplaceWith("state.getOpacity(world, pos)"))
        override fun getOpacity(state: BlockState, world: BlockView, pos: BlockPos): Int {
            val components = getComponents(state)
            // 优先使用显式配置的 light_dampening
            // 如果未配置：方块实体默认 0（接收光照），否则默认 15（阻挡光线）
            val defaultValue = if (components.neteaseBlockEntity != null) 0 else 15
            return components.minecraftLightDampening ?: defaultValue
        }

        /**
         * 控制环境光遮蔽（AO）- 支持 0-15 渐变控制
         * 线性插值：light_dampening 0 → 1.0（无AO），15 → 0.2（正常AO）
         * 默认值：有方块实体时为 0（无AO），否则为 15（正常AO）
         */
        @Deprecated("", ReplaceWith("state.getAmbientOcclusionLightLevel(world, pos)"))
        override fun getAmbientOcclusionLightLevel(state: BlockState, world: BlockView, pos: BlockPos): Float {
            val components = getComponents(state)
            val defaultValue = if (components.neteaseBlockEntity != null) 0 else 15
            val lightDampening = components.minecraftLightDampening ?: defaultValue
            return 1.0f - (lightDampening / 15.0f) * 0.8f
        }
    }
}