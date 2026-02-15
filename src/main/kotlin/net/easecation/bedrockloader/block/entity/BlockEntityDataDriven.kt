package net.easecation.bedrockloader.block.entity

import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
//? if >=1.21.2 {
/*import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
*///?}
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class BlockEntityDataDriven(
    val identifier: Identifier,
    type: BlockEntityType<BlockEntityDataDriven>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    /**
     * 动画管理器，用于管理方块实体的动画播放
     * 在客户端渲染时懒加载创建
     */
    var animationManager: Any? = null

    companion object {
        fun buildBlockEntityType(identifier: Identifier): BlockEntityType<BlockEntityDataDriven> {
            val block = BedrockAddonsRegistry.blocks[identifier]!!
            //? if >=1.21.2 {
            /*return FabricBlockEntityTypeBuilder.create({ pos, state ->
                val type = BedrockAddonsRegistry.blockEntities[identifier]!!
                BlockEntityDataDriven(identifier, type, pos, state)
            }, block).build()
            *///?} else {
            return BlockEntityType.Builder.create({ pos, state ->
                val type = BedrockAddonsRegistry.blockEntities[identifier]!!
                BlockEntityDataDriven(identifier, type, pos, state)
            }, block).build()
            //?}
        }
    }
}