package net.easecation.bedrockloader.block

import com.mojang.serialization.MapCodec
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
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
import net.easecation.bedrockloader.loader.error.LoadingError
import net.easecation.bedrockloader.loader.error.LoadingErrorCollector
import net.easecation.bedrockloader.util.MapColorMatcher
import net.minecraft.block.*
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.enums.BlockHalf
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.Property
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

// 扩展函数：安全地设置可能不存在的属性
fun <T : Comparable<T>> BlockState.withIfExists(property: Property<T>, value: T): BlockState {
    return if (this.contains(property)) {
        this.with(property, value)
    } else {
        this
    }
}

// 重载版本：支持BedrockProperty
//? if >=1.21.2 {
fun <T : Comparable<T>> BlockState.withIfExists(property: BedrockProperty<T>, value: T): BlockState {
    return withIfExists(property.javaProperty, value)
}
//?} else {
/*fun <T : Comparable<T>, P> BlockState.withIfExists(property: BedrockProperty<T, P>, value: T): BlockState
    where P : Property<T>, P : BedrockProperty<T, P> {
    return withIfExists(property.javaProperty, value)
}
*///?}

data class BlockContext(
    val identifier: Identifier,
    val behaviour: BlockBehaviourDefinition.BlockBehaviour,
    //? if >=1.21.2 {
    val properties: Map<String, BedrockProperty<*>>,
    //?} else {
    /*val properties: Map<String, BedrockProperty<*, *>>,
    *///?}
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

        //? if >=1.21.4 {
        /**
         * 1.21.4: 使用预配置的settings创建Block（settings已包含registry key）
         */
        fun createWithSettings(identifier: Identifier, behaviour: BlockBehaviourDefinition.BlockBehaviour, baseSettings: Settings): BlockDataDriven {
            val components = behaviour.components

            // 复制baseSettings并应用自定义配置
            val settings = baseSettings.hardness(4.0f)  // TODO hardness

            // 应用所有自定义设置（与calculateSettings()相同的逻辑）
            val hasCustomGeometry = components.minecraftGeometry?.let { geometry ->
                val geometryId = when (geometry) {
                    is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                    is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                }
                geometryId != "minecraft:geometry.full_block"
            } ?: false
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

            components.minecraftMapColor?.let { mapColor ->
                val rgb = mapColor.getColor()
                val javaMapColor = MapColorMatcher.findClosestMapColor(rgb)
                settings.mapColor(javaMapColor)
            }

            // 计算properties
            fun calculateProperties(): Map<String, BedrockProperty<*>> {
                val placementDirection: Map<String, BedrockProperty<*>> = behaviour.description.traits?.minecraftPlacementDirection?.enabled_states?.mapNotNull { state ->
                    if (state == null) {
                        LoadingErrorCollector.addWarning(
                            source = identifier.toString(),
                            phase = LoadingError.Phase.BLOCK_REGISTER,
                            message = "minecraft:placement_direction trait 包含未知的 enabled_states 值，将被忽略。支持的值: minecraft:cardinal_direction, minecraft:facing_direction"
                        )
                        return@mapNotNull null
                    }
                    when (state) {
                        TraitPlacementDirection.State.MINECRAFT_CARDINAL_DIRECTION -> MINECRAFT_CARDINAL_DIRECTION
                        TraitPlacementDirection.State.MINECRAFT_FACING_DIRECTION -> MINECRAFT_FACING_DIRECTION
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()

                val placementPosition: Map<String, BedrockProperty<*>> = behaviour.description.traits?.minecraftPlacementPosition?.enabled_states?.mapNotNull { state ->
                    if (state == null) {
                        LoadingErrorCollector.addWarning(
                            source = identifier.toString(),
                            phase = LoadingError.Phase.BLOCK_REGISTER,
                            message = "minecraft:placement_position trait 包含未知的 enabled_states 值，将被忽略。支持的值: minecraft:block_face, minecraft:vertical_half"
                        )
                        return@mapNotNull null
                    }
                    when (state) {
                        TraitPlacementPosition.State.MINECRAFT_BLOCK_FACE -> MINECRAFT_BLOCK_FACE
                        TraitPlacementPosition.State.MINECRAFT_VERTICAL_HALF -> MINECRAFT_VERTICAL_HALF
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()

                val faceDirectional: Map<String, BedrockProperty<*>> = when (components.neteaseFaceDirectional?.type) {
                    FaceDirectionalType.direction -> mapOf(BlockContext.DIRECTION.getBedrockName() to BlockContext.DIRECTION)
                    FaceDirectionalType.facing_direction -> mapOf(BlockContext.FACING_DIRECTION.getBedrockName() to BlockContext.FACING_DIRECTION)
                    else -> emptyMap()
                }

                val properties: Map<String, BedrockProperty<*>> = behaviour.description.states?.mapValues { (key, state) ->
                    when (state) {
                        is StateBoolean -> BedrockBooleanProperty.of(key, state.toSet())
                        is StateInt -> BedrockIntProperty.of(key, state.toSet())
                        is StateString -> BedrockStringProperty.of(key, state.toSet())
                        is StateRange -> BedrockIntProperty.of(key, (state.values.min..state.values.max).toSet())
                    }
                } ?: emptyMap()
                return placementDirection + placementPosition + faceDirectional + properties
            }

            val properties = calculateProperties()

            // 1.21.4: 预验证属性名不重复
            // 必须在构造 Block 之前检查，因为 Block 构造函数会创建 intrusive holder，
            // 一旦构造函数内部（appendProperties）抛出异常，holder 就成为孤儿导致崩溃
            val seenJavaNames = mutableSetOf<String>()
            for ((bedrockName, prop) in properties) {
                val javaName = prop.javaProperty.name
                if (!seenJavaNames.add(javaName)) {
                    throw IllegalArgumentException(
                        "Block $identifier has duplicate property name '$javaName' (bedrock key: $bedrockName). " +
                        "This must be detected before Block construction to prevent intrusive holder leak in 1.21.4."
                    )
                }
            }

            return BlockContext(identifier, behaviour, properties).BlockDataDriven(settings)
        }
        //?}

        fun create(identifier: Identifier, behaviour: BlockBehaviourDefinition.BlockBehaviour): BlockDataDriven {
            val components = behaviour.components

            //? if >=1.21.2 {
            fun calculateProperties(): Map<String, BedrockProperty<*>> {
                val placementDirection: Map<String, BedrockProperty<*>> = behaviour.description.traits?.minecraftPlacementDirection?.enabled_states?.mapNotNull { state ->
            //?} else {
            /*fun calculateProperties(): Map<String, BedrockProperty<*, *>> {
                val placementDirection: Map<String, BedrockProperty<*, *>> = behaviour.description.traits?.minecraftPlacementDirection?.enabled_states?.mapNotNull { state ->
            *///?}
                    if (state == null) {
                        LoadingErrorCollector.addWarning(
                            source = identifier.toString(),
                            phase = LoadingError.Phase.BLOCK_REGISTER,
                            message = "minecraft:placement_direction trait 包含未知的 enabled_states 值，将被忽略。支持的值: minecraft:cardinal_direction, minecraft:facing_direction"
                        )
                        return@mapNotNull null
                    }
                    when (state) {
                        TraitPlacementDirection.State.MINECRAFT_CARDINAL_DIRECTION -> MINECRAFT_CARDINAL_DIRECTION
                        TraitPlacementDirection.State.MINECRAFT_FACING_DIRECTION -> MINECRAFT_FACING_DIRECTION
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()
                //? if >=1.21.2 {
                val placementPosition: Map<String, BedrockProperty<*>> = behaviour.description.traits?.minecraftPlacementPosition?.enabled_states?.mapNotNull { state ->
                //?} else {
                /*val placementPosition: Map<String, BedrockProperty<*, *>> = behaviour.description.traits?.minecraftPlacementPosition?.enabled_states?.mapNotNull { state ->
                *///?}
                    if (state == null) {
                        LoadingErrorCollector.addWarning(
                            source = identifier.toString(),
                            phase = LoadingError.Phase.BLOCK_REGISTER,
                            message = "minecraft:placement_position trait 包含未知的 enabled_states 值，将被忽略。支持的值: minecraft:block_face, minecraft:vertical_half"
                        )
                        return@mapNotNull null
                    }
                    when (state) {
                        TraitPlacementPosition.State.MINECRAFT_BLOCK_FACE -> MINECRAFT_BLOCK_FACE
                        TraitPlacementPosition.State.MINECRAFT_VERTICAL_HALF -> MINECRAFT_VERTICAL_HALF
                    }
                }?.associateBy { it.getBedrockName() } ?: emptyMap()
                //? if >=1.21.2 {
                val faceDirectional: Map<String, BedrockProperty<*>> = behaviour.components.neteaseFaceDirectional?.type?.let { type ->
                //?} else {
                /*val faceDirectional: Map<String, BedrockProperty<*, *>> = behaviour.components.neteaseFaceDirectional?.type?.let { type ->
                *///?}
                    mapOf(type.name to when (type) {
                        FaceDirectionalType.direction -> DIRECTION
                        FaceDirectionalType.facing_direction -> FACING_DIRECTION
                    })
                } ?: emptyMap()
                //? if >=1.21.2 {
                val properties: Map<String, BedrockProperty<*>> = behaviour.description.states?.mapValues { (key, state) ->
                //?} else {
                /*val properties: Map<String, BedrockProperty<*, *>> = behaviour.description.states?.mapValues { (key, state) ->
                *///?}
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
                // Full block（无自定义 geometry 或 minecraft:geometry.full_block）：默认启用面剔除，确保性能优化
                // 有自定义 geometry、透明渲染或方块实体：禁用面剔除
                val hasCustomGeometry = components.minecraftGeometry?.let { geometry ->
                    val geometryId = when (geometry) {
                        is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                        is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                    }
                    // minecraft:geometry.full_block 是标准立方体，不算自定义 geometry
                    geometryId != "minecraft:geometry.full_block"
                } ?: false
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
                // 应用 map_color: 将基岩版颜色匹配到最接近的 Java 版 MapColor
                components.minecraftMapColor?.let { mapColor ->
                    val rgb = mapColor.getColor()
                    val javaMapColor = MapColorMatcher.findClosestMapColor(rgb)
                    settings.mapColor(javaMapColor)
                }
                return settings
            }

            val settings = calculateSettings()
            val properties = calculateProperties()
            return BlockContext(identifier, behaviour, properties).BlockDataDriven(settings)
        }
    }

    inner class BlockDataDriven(settings: Settings) : BlockWithEntity(settings) {

        private val orSplitRegex = """\s*\|\|\s*""".toRegex()
        private val andSplitRegex = """\s*&&\s*""".toRegex()
        private val blockStateRegex = """^\s*q(uery)?\s*\.\s*block_state\s*\(\s*'(?<key>[^']+)'\s*\)\s*(?<operator>==|!=)\s*'(?<value>[^']+)'\s*$""".toRegex()

        private val componentsByState: Map<BlockState, BlockComponents>

        /**
         * 获取外部 BlockContext 实例
         * 用于导出方块状态映射时访问属性定义
         */
        fun getBlockContext(): BlockContext = this@BlockContext

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
                val satisfied = evalCondition(condition, state)
                if (satisfied) {
                    activated = activated.mergeComponents(components)
                }
            }
            return activated
        }

        /**
         * 评估 Molang 条件表达式
         * 支持 || (或) 和 && (与) 运算符
         * 运算符优先级: && 高于 ||
         * 即: a && b || c && d 会被解析为 (a && b) || (c && d)
         */
        private fun evalCondition(condition: String, state: BlockState): Boolean {
            // 按 || 拆分成多个 "或组"
            val orGroups = orSplitRegex.split(condition)

            // 任意一个或组满足即可 (OR 逻辑)
            return orGroups.any { orGroup ->
                // 每个或组内按 && 拆分成多个条件
                val andConditions = andSplitRegex.split(orGroup)
                // 组内所有条件都需满足 (AND 逻辑)
                andConditions.all { evalBlockStateCondition(it.trim(), state) }
            }
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
                //? if >=1.21.4 {
                FaceDirectionalType.direction -> Direction.byId(state[DIRECTION] + 2)  // 水平方向从NORTH=2开始
                //?} else {
                /*FaceDirectionalType.direction -> Direction.fromHorizontal(state[DIRECTION])
                *///?}
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
                //? if >=1.21.4 {
                FaceDirectionalType.direction -> Direction.byId(state[DIRECTION] + 2)  // 水平方向从NORTH=2开始
                //?} else {
                /*FaceDirectionalType.direction -> Direction.fromHorizontal(state[DIRECTION])
                *///?}
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
                .withIfExists(DIRECTION,
                    //? if >=1.21.4 {
                    ctx.horizontalPlayerFacing.opposite.id - 2  // 水平方向从NORTH=2开始,减2得到0-3索引
                    //?} else {
                    /*ctx.horizontalPlayerFacing.opposite.horizontal
                    *///?}
                )
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
        //? if >=1.21.2 {
        @Deprecated("", ReplaceWith("state.getOpacity(world, pos)"))
        override fun getOpacity(state: BlockState): Int {
            val components = getComponents(state)
            // 优先使用显式配置的 light_dampening
            // 如果未配置：方块实体默认 0（接收光照），否则默认 15（阻挡光线）
            val defaultValue = if (components.neteaseBlockEntity != null) 0 else 15
            return components.minecraftLightDampening ?: defaultValue
        }
        //?} else {
        /*@Deprecated("", ReplaceWith("state.getOpacity(world, pos)"))
        override fun getOpacity(state: BlockState, world: BlockView, pos: BlockPos): Int {
            val components = getComponents(state)
            // 优先使用显式配置的 light_dampening
            // 如果未配置：方块实体默认 0（接收光照），否则默认 15（阻挡光线）
            val defaultValue = if (components.neteaseBlockEntity != null) 0 else 15
            return components.minecraftLightDampening ?: defaultValue
        }
        *///?}

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

        /**
         * 控制面剔除 - 判断方块的剔除形状是否为完整立方体
         * 当两个相邻方块都返回 true 时，它们接触的面会被剔除
         */
        //? if <1.21.2 {
        /*@Deprecated("", ReplaceWith("state.exceedsCube()"))
        override fun isCullingShapeFullCube(state: BlockState, world: BlockView, pos: BlockPos): Boolean {
            val components = getComponents(state)
            val geometry = components.minecraftGeometry
            val geometryId = when (geometry) {
                is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                null -> null
            }

            // 检查是否为完整立方体（无 geometry 或 minecraft:geometry.full_block）
            val isFullBlock = geometryId == null || geometryId == "minecraft:geometry.full_block"

            // 检查渲染方法是否为不透明
            val renderMethod = components.minecraftMaterialInstances?.get("*")?.render_method
            val isOpaque = renderMethod == null || renderMethod == ComponentMaterialInstances.RenderMethod.opaque

            // 检查是否有方块实体（方块实体通常需要特殊渲染）
            val hasBlockEntity = components.neteaseBlockEntity != null

            return isFullBlock && isOpaque && !hasBlockEntity
        }
        *///?}
    }
}