package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.fabric.api.client.model.ModelProviderContext
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.util.Identifier

object BedrockVariantProvider : ModelVariantProvider {
    override fun loadModelVariant(modelId: ModelIdentifier, context: ModelProviderContext): UnbakedModel? {
        if (modelId.variant.equals("inventory")) {
            val identifier = Identifier(modelId.namespace, modelId.path)
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