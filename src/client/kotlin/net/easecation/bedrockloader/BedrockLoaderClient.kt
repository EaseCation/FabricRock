package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader.context
import net.easecation.bedrockloader.loader.BedrockResourcePackLoader
import net.easecation.bedrockloader.render.BedrockModelLoadingPlugin
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Environment(EnvType.CLIENT)
object BedrockLoaderClient : ClientModInitializer {

    val logger: Logger = LoggerFactory.getLogger("bedrock-loader")

    override fun onInitializeClient() {
        ModelLoadingPlugin.register(BedrockModelLoadingPlugin)

        // load resource pack
        logger.info("Loading resource pack...")
        val resourcePackLoader = BedrockResourcePackLoader(BedrockLoader.getTmpResourceDir(), context)
        resourcePackLoader.load()
    }

}