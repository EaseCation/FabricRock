package net.easecation.bedrockloader.block

import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.util.Identifier

class BlockDataDriven private constructor(val identifier: Identifier, val components: BlockComponents, settings: Settings) : Block(settings) {

    companion object {
        fun create(identifier: Identifier, components: BlockComponents): BlockDataDriven {
            // 在这里进行逻辑计算
            val settings = calculateSettings(components)
            return BlockDataDriven(identifier, components, settings)
        }

        private fun calculateSettings(components: BlockComponents): Settings {
            val settings = FabricBlockSettings.of(Material.METAL).hardness(4.0f)
            components.minecraftLightEmission?.let {
                settings.luminance(it)
            }
            return settings
        }
    }

    /*override fun getCollisionShape(world: BlockView?, pos: BlockPos?, context: ShapeContext?): VoxelShape {
        if (components.minecraftCollisionBox != null) {
            val it = components.minecraftCollisionBox
            return VoxelShapes.cuboid(it.origin[0].toDouble(), it.origin[1].toDouble(), it.origin[2].toDouble(), it.origin[0].toDouble() + it.size[0].toDouble(), it.origin[1].toDouble() + it.size[1].toDouble(), it.origin[2].toDouble() + it.size[2].toDouble())
        } else {
            return super.getCollisionShape(world, pos, context)
        }
    }*/
}