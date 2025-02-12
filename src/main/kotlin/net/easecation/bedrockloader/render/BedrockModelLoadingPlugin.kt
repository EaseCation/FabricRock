package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.loader.BedrockAddonsLoader
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
                    val geometry = BedrockAddonsLoader.context.behavior.blocks[identifier]?.components?.minecraftGeometry ?: return@register null
                    val geometryIdentifier = when (geometry) {
                        is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                        is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                    }
                    BedrockAddonsRegistry.models[geometryIdentifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("item/".length))
                    val geometry = BedrockAddonsLoader.context.behavior.blocks[identifier]?.components?.minecraftGeometry ?: return@register null
                    val geometryIdentifier = when (geometry) {
                        is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                        is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
                    }
                    BedrockAddonsRegistry.models[geometryIdentifier]
                }
                else -> null
            }
        }
    }
}