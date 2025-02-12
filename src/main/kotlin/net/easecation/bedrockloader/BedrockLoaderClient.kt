package net.easecation.bedrockloader

import net.easecation.bedrockloader.render.BedrockModelLoadingPlugin
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin

@Environment(EnvType.CLIENT)
object BedrockLoaderClient : ClientModInitializer {

    override fun onInitializeClient() {
        ModelLoadingPlugin.register(BedrockModelLoadingPlugin)
    }

}