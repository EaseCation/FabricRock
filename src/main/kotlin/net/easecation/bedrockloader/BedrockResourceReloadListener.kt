package net.easecation.bedrockloader

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import net.easecation.bedrockloader.util.identifierOf

class BedrockResourceReloadListener : SimpleSynchronousResourceReloadListener {

    override fun getFabricId(): Identifier {
        return identifierOf("bedrock-loader")
    }

    override fun reload(manager: ResourceManager?) {
        BedrockLoader.logger.info("Reloading BedrockLoader resources")
        if (manager == null) {
            BedrockLoader.logger.error("Resource manager is null!")
            return
        }
        // val resources = manager.findResources("textures") {path -> path.endsWith(".png") }
        // resources?.forEach { resource -> BedrockLoader.logger.info("Found resource: $resource") }
        // 打印manager的class名称
        BedrockLoader.logger.info("Resource manager class: ${manager.javaClass.name}")
        manager.streamResourcePacks().forEach { pack ->
            //? if >=1.21 {
            /*BedrockLoader.logger.info("Found resource pack: ${pack.id}, class: ${pack.javaClass.name}")
            *///?} else {
            BedrockLoader.logger.info("Found resource pack: ${pack.name}, class: ${pack.javaClass.name}")
            //?}
        }
        manager.getResource(identifierOf("bedrock-loader", "textures/block/test_block.png")).ifPresent {
            resource -> BedrockLoader.logger.info("Found resource: $resource")
        }
    }

}