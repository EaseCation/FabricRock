package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.block.BlockDataDriven
import net.easecation.bedrockloader.deserializer.BedrockPackContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.BlockItem
import net.minecraft.util.registry.Registry

class BedrockBehaviorPackLoader(
        val context: BedrockPackContext
) {

    fun load() {
        val env = FabricLoader.getInstance().environmentType
        // Block
        context.behavior.blocks.forEach { (id, beh) ->
            val block = BlockDataDriven.create(id, beh.components)
            Registry.register(Registry.BLOCK, id, block)
            BedrockAddonsRegistry.blocks[id] = block
            val item = BlockItem(block, FabricItemSettings())
            Registry.register(Registry.ITEM, id, item)
            BedrockAddonsRegistry.items[id] = item
        }
        // Entity
        context.behavior.entities.forEach { (id, beh) ->
            // entity type
            val entityType = BedrockAddonsRegistry.getOrRegisterEntityType(id)
            BedrockAddonsRegistry.entityComponents[id] = beh.components
            // entity attributes
            FabricDefaultAttributeRegistry.register(entityType, EntityDataDriven.buildEntityAttributes(beh.components))
        }
    }

}