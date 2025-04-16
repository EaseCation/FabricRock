package net.easecation.bedrockloader.block.entity

import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

class BlockEntityDataDriven(
    val identifier: Identifier,
    type: BlockEntityType<BlockEntityDataDriven>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    companion object {
        fun buildBlockEntityType(identifier: Identifier): BlockEntityType<BlockEntityDataDriven> {
            val block = BedrockAddonsRegistry.blocks[identifier]!!
            return BlockEntityType.Builder.create({ pos, state ->
                val type = BedrockAddonsRegistry.blockEntities[identifier]!!
                BlockEntityDataDriven(identifier, type, pos, state)
            }, block).build()
        }
    }
}