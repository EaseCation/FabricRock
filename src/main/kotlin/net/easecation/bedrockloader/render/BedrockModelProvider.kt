package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.fabric.api.client.model.ModelProviderContext
import net.fabricmc.fabric.api.client.model.ModelResourceProvider
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.Identifier

object BedrockModelProvider : ModelResourceProvider {

    override fun loadModelResource(resourceId: Identifier, context: ModelProviderContext): UnbakedModel? {
        return BedrockAddonsRegistry.models[resourceId.path]?.let { return it }
    }

}