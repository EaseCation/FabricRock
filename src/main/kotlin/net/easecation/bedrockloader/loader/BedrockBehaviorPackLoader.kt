package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.block.BlockDataDriven
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class BedrockBehaviorPackLoader(
        val context: BedrockBehaviorContext
) {

    val registeredItems: MutableMap<Identifier, Item> = mutableMapOf()
    val registeredBlocks: MutableMap<Identifier, Block> = mutableMapOf()

    fun load() {
        context.blocks.forEach { (id, beh) ->
            val block = BlockDataDriven.create(id, beh.components)
            Registry.register(Registry.BLOCK, id, block)
            registeredBlocks[id] = block
            val item = BlockItem(block, FabricItemSettings())
            Registry.register(Registry.ITEM, id, item)
            registeredItems[id] = item
        }
    }

}