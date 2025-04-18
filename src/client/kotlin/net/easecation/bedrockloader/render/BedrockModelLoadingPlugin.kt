package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.util.Identifier

object BedrockModelLoadingPlugin : ModelLoadingPlugin {
    override fun onInitializeModelLoader(pluginContext: ModelLoadingPlugin.Context) {
        pluginContext.resolveModel().register { context ->
            val id = context.id()
            when {
                id.path.startsWith("block/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistryClient.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistryClient.itemModels[identifier]
                }
                else -> null
            }
        }

        BedrockAddonsRegistry.blocks.forEach { (id, v) ->
            val block = v as? BlockContext.BlockDataDriven ?: return@forEach
            pluginContext.registerBlockStateResolver(block) { context ->
                val loadedUnbakedModel = context.getOrLoadModel(id.withPath { "block/${it}" })
                block.stateManager.states.forEach {
                    val model = (loadedUnbakedModel as? BedrockGeometryModel)?.getModelVariant(block, it) ?: loadedUnbakedModel
                    context.setModel(it, model)
                }
            }
        }
    }
}