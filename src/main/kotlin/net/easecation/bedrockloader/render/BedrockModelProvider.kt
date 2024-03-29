package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.fabric.api.client.model.ModelProviderContext
import net.fabricmc.fabric.api.client.model.ModelResourceProvider
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.Identifier

object BedrockModelProvider : ModelResourceProvider {

    override fun loadModelResource(resourceId: Identifier, context: ModelProviderContext): UnbakedModel? {
        if (resourceId.path.startsWith("block/")) {
            val identifier = Identifier(resourceId.namespace, resourceId.path.substring("block/".length))
            val geometry = BedrockAddonsLoader.context.behavior.blocks[identifier]?.components?.minecraftGeometry ?: return null
            val geometryIdentifier = when (geometry) {
                is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
            }
            return BedrockAddonsRegistry.models[geometryIdentifier]
        } else {
            return null
        }
    }

}