package net.easecation.bedrockloader.block

import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.ComponentSelectionBox
import net.easecation.bedrockloader.bedrock.block.state.StateBoolean
import net.easecation.bedrockloader.bedrock.block.state.StateInt
import net.easecation.bedrockloader.bedrock.block.state.StateRange
import net.easecation.bedrockloader.bedrock.block.state.StateString
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementDirection
import net.easecation.bedrockloader.bedrock.block.traits.TraitPlacementPosition
import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.block.enums.BlockHalf
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Property
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

data class BlockContext(
    val identifier: Identifier,
    val behaviour: BlockBehaviourDefinition.BlockBehaviour,
    val states: Map<String, Property<*>>
) {
    companion object {
        // minecraft:placement_direction
        val MINECRAFT_CARDINAL_DIRECTION = DirectionProperty.of("minecraft_cardinal_direction", Direction.Type.HORIZONTAL)
        val MINECRAFT_FACING_DIRECTION = DirectionProperty.of("minecraft_facing_direction", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN)
        // minecraft:placement_position
        val MINECRAFT_BLOCK_FACE = DirectionProperty.of("minecraft_block_face", Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN)
        val MINECRAFT_VERTICAL_HALF = EnumProperty.of("minecraft_vertical_half", BlockHalf::class.java)

        fun create(identifier: Identifier, behaviour: BlockBehaviourDefinition.BlockBehaviour): BlockDataDriven {
            // 在这里进行逻辑计算
            val settings = calculateSettings(behaviour)
            val states = calculateStates(behaviour)
            return BlockContext(identifier, behaviour, states).BlockDataDriven(settings)
        }

        private fun calculateStates(behaviour: BlockBehaviourDefinition.BlockBehaviour): Map<String, Property<*>> {
            val placementDirection = behaviour.description.traits?.minecraftPlacementDirection?.enabled_states?.map { state ->
                when (state) {
                    TraitPlacementDirection.State.MINECRAFT_CARDINAL_DIRECTION -> MINECRAFT_CARDINAL_DIRECTION
                    TraitPlacementDirection.State.MINECRAFT_FACING_DIRECTION -> MINECRAFT_FACING_DIRECTION
                }
            }?.associateBy { it.name } ?: emptyMap()
            val placementPosition = behaviour.description.traits?.minecraftPlacementPosition?.enabled_states?.map { state ->
                when (state) {
                    TraitPlacementPosition.State.MINECRAFT_BLOCK_FACE -> MINECRAFT_BLOCK_FACE
                    TraitPlacementPosition.State.MINECRAFT_VERTICAL_HALF -> MINECRAFT_VERTICAL_HALF
                }
            }?.associateBy { it.name } ?: emptyMap()
            val states = behaviour.description.states?.map { (key, state) ->
                val name = key.replace(':', '_').lowercase()
                when (state) {
                    is StateBoolean -> BedrockBooleanProperty.of(name, state.toSet())
                    is StateInt -> BedrockIntProperty.of(name, state.toSet())
                    is StateString -> BedrockStringProperty.of(name, state.toSet())
                    is StateRange -> BedrockIntProperty.of(name, (state.values.min..state.values.max).toSet())
                }
            }?.associateBy { it.name } ?: emptyMap()
            return placementDirection + placementPosition + states
        }

        private fun calculateSettings(behaviour: BlockBehaviourDefinition.BlockBehaviour): Settings {
            val settings = Settings.create().hardness(4.0f).nonOpaque()  // TODO hardness
            behaviour.components.minecraftCollisionBox?.let {
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
            behaviour.components.minecraftLightEmission?.let {
                settings.luminance { _ -> it }
            }
            return settings
        }
    }

    inner class BlockDataDriven(settings: Settings) : Block(settings) {
        init {
            defaultState = stateManager.defaultState
                .withIfExists(MINECRAFT_CARDINAL_DIRECTION, Direction.SOUTH)
                .withIfExists(MINECRAFT_FACING_DIRECTION, Direction.DOWN)
                .withIfExists(MINECRAFT_BLOCK_FACE, Direction.DOWN)
                .withIfExists(MINECRAFT_VERTICAL_HALF, BlockHalf.BOTTOM)
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
        }

        override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
            states.values.forEach { builder.add(it) }
        }

        override fun getCollisionShape(
            state: BlockState,
            world: BlockView,
            pos: BlockPos,
            context: ShapeContext
        ): VoxelShape = when {
            this.collidable -> when (val box = behaviour.components.minecraftCollisionBox) {
                is ComponentCollisionBox.ComponentCollisionBoxBoolean -> getOutlineShape(state, world, pos, context)
                is ComponentCollisionBox.ComponentCollisionBoxCustom -> VoxelShapes.cuboid(
                    (box.origin[0].toDouble() + 8) / 16,
                    box.origin[1].toDouble() / 16,
                    (box.origin[2].toDouble() + 8) / 16,
                    (box.origin[0].toDouble() + 8) / 16 + box.size[0].toDouble() / 16,
                    box.origin[1].toDouble() / 16 + box.size[1].toDouble() / 16,
                    (box.origin[2].toDouble() + 8) / 16 + box.size[2].toDouble() / 16
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
        ): VoxelShape = when (val box = behaviour.components.minecraftSelectionBox) {
            is ComponentSelectionBox.ComponentSelectionBoxBoolean -> super.getOutlineShape(state, world, pos, context)
            is ComponentSelectionBox.ComponentSelectionBoxCustom -> VoxelShapes.cuboid(
                (box.origin[0].toDouble() + 8) / 16,
                box.origin[1].toDouble() / 16,
                (box.origin[2].toDouble() + 8) / 16,
                (box.origin[0].toDouble() + 8) / 16 + box.size[0].toDouble() / 16,
                box.origin[1].toDouble() / 16 + box.size[1].toDouble() / 16,
                (box.origin[2].toDouble() + 8) / 16 + box.size[2].toDouble() / 16
            )
            else -> super.getOutlineShape(state, world, pos, context)
        }
    }
}