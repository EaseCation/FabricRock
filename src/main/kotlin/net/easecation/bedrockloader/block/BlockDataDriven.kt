package net.easecation.bedrockloader.block

import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

class BlockDataDriven private constructor(val identifier: Identifier, val components: BlockComponents, settings: Settings) : Block(settings) {

    companion object {
        fun create(identifier: Identifier, components: BlockComponents): BlockDataDriven {
            // 在这里进行逻辑计算
            val settings = calculateSettings(components)
            return BlockDataDriven(identifier, components, settings)
        }

        private fun calculateSettings(components: BlockComponents): Settings {
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
            // TODO SelectionBox
            components.minecraftLightEmission?.let {
                settings.luminance { _ -> it }
            }
            return settings
        }
    }

    override fun getOutlineShape(state: BlockState?, view: BlockView?, pos: BlockPos?, context: ShapeContext?): VoxelShape {
        if (components.minecraftCollisionBox != null) {
            val it = components.minecraftCollisionBox
            when (it) {
                is ComponentCollisionBox.ComponentCollisionBoxBoolean -> {
                    return super.getOutlineShape(state, view, pos, context)
                }
                is ComponentCollisionBox.ComponentCollisionBoxCustom -> {
                    return VoxelShapes.cuboid(
                            (it.origin[0].toDouble() + 8) / 16,
                            it.origin[1].toDouble() / 16,
                            (it.origin[2].toDouble() + 8) / 16,
                            (it.origin[0].toDouble() + 8) / 16 + it.size[0].toDouble() / 16,
                            it.origin[1].toDouble() / 16 + it.size[1].toDouble() / 16,
                            (it.origin[2].toDouble() + 8) / 16 + it.size[2].toDouble() / 16
                    )
                }
            }
        } else {
            return super.getOutlineShape(state, view, pos, context)
        }
    }
}