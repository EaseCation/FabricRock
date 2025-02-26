package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.SpawnEggItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class BedrockBehaviorPackLoader(
        val context: BedrockPackContext
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun load() {
        // Block
        context.behavior.blocks.forEach { (id, beh) ->
            BedrockLoader.logger.info("Registering block $id")
            val block = BlockContext.create(id, beh)
            Registry.register(Registries.BLOCK, id, block)
            BedrockAddonsRegistry.blocks[id] = block
            val item = BlockItem(block, Item.Settings())
            Registry.register(Registries.ITEM, id, item)
            BedrockAddonsRegistry.items[id] = item
            // Block Entity
            beh.components.neteaseBlockEntity?.let { blockEntity ->
                BedrockLoader.logger.info("Registering block entity $id")
                val blockEntityType = BlockEntityDataDriven.buildBlockEntityType(id)
                Registry.register(Registries.BLOCK_ENTITY_TYPE, id, blockEntityType)
                BedrockAddonsRegistry.blockEntities[id] = blockEntityType
            }
        }
        // Entity
        context.behavior.entities.forEach { (id, beh) ->
            BedrockLoader.logger.info("Registering entity $id")
            // entity type
            val entityType = EntityDataDriven.buildEntityType(id)
            Registry.register(Registries.ENTITY_TYPE, id, entityType)
            BedrockAddonsRegistry.entities[id] = entityType
            BedrockAddonsRegistry.entityComponents[id] = beh.components
            // entity attributes
            FabricDefaultAttributeRegistry.register(entityType, EntityDataDriven.buildEntityAttributes(beh.components))
            // spawn egg
            if (beh.description.is_spawnable == true) {
                val clientEntity = context.resource.entities[id]?.description
                val entityName = id.path
                val itemIdentifier = Identifier(id.namespace, "${entityName}_spawn_egg")
                val spawnEggItem = SpawnEggItem(
                    entityType,
                    clientEntity?.spawn_egg?.base_color?.replace("#", "")?.hexToInt(HexFormat.Default) ?: 0xffffff,
                    clientEntity?.spawn_egg?.overlay_color?.replace("#", "")?.hexToInt(HexFormat.Default) ?: 0xffffff,
                    Item.Settings()
                )
                Registry.register(Registries.ITEM, itemIdentifier, spawnEggItem)
                BedrockAddonsRegistry.items[itemIdentifier] = spawnEggItem
            }
        }
    }

}