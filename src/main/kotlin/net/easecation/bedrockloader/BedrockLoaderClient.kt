package net.easecation.bedrockloader

import net.easecation.bedrockloader.render.BedrockModelProvider
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry

@Environment(EnvType.CLIENT)
object BedrockLoaderClient : ClientModInitializer {

    override fun onInitializeClient() {
        ModelLoadingRegistry.INSTANCE.registerResourceProvider { rm -> BedrockModelProvider }
    }

}