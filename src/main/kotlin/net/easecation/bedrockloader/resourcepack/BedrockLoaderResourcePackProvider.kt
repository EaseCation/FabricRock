package net.easecation.bedrockloader.resourcepack

import net.easecation.bedrockloader.BedrockLoader
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator
import net.minecraft.resource.*
import net.minecraft.resource.DirectoryResourcePack.DirectoryBackedFactory
import net.minecraft.resource.ZipResourcePack.ZipBackedFactory
import net.minecraft.text.Text
import java.io.File
import java.util.*
import java.util.function.Consumer

class BedrockLoaderResourcePackProvider : ResourcePackProvider {

    private val packsFolder: File = BedrockLoader.getTmpResourceDir()

    override fun register(consumer: Consumer<ResourcePackProfile>) {
        if (!packsFolder.isDirectory) {
            packsFolder.mkdir()
        }

        val metadata = ResourcePackInfo(
            "bedrock-loader",
            Text.translatable("pack.name.bedrock-loader"),
            ModResourcePackCreator.RESOURCE_PACK_SOURCE,
            Optional.empty()
        )
        ResourcePackProfile.create(
            metadata,
            if (packsFolder.isDirectory) DirectoryBackedFactory(packsFolder.toPath()) else ZipBackedFactory(packsFolder),
            ResourceType.CLIENT_RESOURCES,
            ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false)
        )?.let { consumer.accept(it) }
    }
}