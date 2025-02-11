package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.block.BlockDataDriven
import net.easecation.bedrockloader.deserializer.BedrockPackContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

class BedrockBehaviorPackLoader(
        val context: BedrockPackContext
) {

    fun load() {
        val env = FabricLoader.getInstance().environmentType
        // Block
        context.behavior.blocks.forEach { (id, beh) ->
            val block = BlockDataDriven.create(id, beh.components)
            Registry.register(Registries.BLOCK, id, block)
            BedrockAddonsRegistry.blocks[id] = block
            if (env == EnvType.CLIENT) {
                beh.components.minecraftMaterialInstances?.let { materialInstances ->
                    val renderMethod = materialInstances["*"]?.render_method ?: return@let
                    if (renderMethod == ComponentMaterialInstances.RenderMethod.alpha_test) {
                        BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())
                    } else if (renderMethod == ComponentMaterialInstances.RenderMethod.blend) {
                        BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent())
                    }
                }
            }
            val item = BlockItem(block, Item.Settings())
            Registry.register(Registries.ITEM, id, item)
            BedrockAddonsRegistry.items[id] = item
        }
        // Entity
        context.behavior.entities.forEach { (id, beh) ->
            BedrockLoader.logger.info("Registering entity $id")
            // entity type
            val entityType = BedrockAddonsRegistry.getOrRegisterEntityType(id)
            BedrockAddonsRegistry.entityComponents[id] = beh.components
            // entity attributes
            FabricDefaultAttributeRegistry.register(entityType, EntityDataDriven.buildEntityAttributes(beh.components))
        }
    }

}