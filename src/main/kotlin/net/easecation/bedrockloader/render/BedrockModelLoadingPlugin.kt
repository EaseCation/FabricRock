package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.util.Identifier

object BedrockModelLoadingPlugin : ModelLoadingPlugin {
    override fun onInitializeModelLoader(pluginContext: ModelLoadingPlugin.Context) {
        pluginContext.resolveModel().register { context ->
            val id = context.id()
            when {
                id.path.startsWith("block/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistry.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistry.itemModels[identifier]
                }
                else -> null
            }
        }

        BedrockAddonsRegistry.blocks.forEach { (id, block) ->
            pluginContext.registerBlockStateResolver(block) { context ->
                val model = context.getOrLoadModel(id.withPath { "block/${it}" })
                context.block().stateManager.states.forEach {
                    context.setModel(it, model)
                }
            }
        }
    }
}