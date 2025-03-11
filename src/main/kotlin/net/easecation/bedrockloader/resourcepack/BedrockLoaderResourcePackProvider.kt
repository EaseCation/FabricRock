package net.easecation.bedrockloader.resourcepack

import net.easecation.bedrockloader.BedrockLoader
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator
import net.minecraft.resource.*
import net.minecraft.resource.DirectoryResourcePack.DirectoryBackedFactory
import net.minecraft.resource.ZipResourcePack.ZipBackedFactory
import net.minecraft.text.Text
import java.io.File
import java.util.function.Consumer

class BedrockLoaderResourcePackProvider : ResourcePackProvider {

    private val packsFolder: File = BedrockLoader.getTmpResourceDir()

    override fun register(consumer: Consumer<ResourcePackProfile>) {
        if (!packsFolder.isDirectory) {
            packsFolder.mkdir()
        }

        ResourcePackProfile.create(
            "bedrock-loader-resource",
            Text.translatable("pack.name.bedrock-loader"),
            true,
            if (packsFolder.isDirectory) DirectoryBackedFactory(packsFolder.toPath(), false) else ZipBackedFactory(packsFolder, false),
            ResourceType.CLIENT_RESOURCES,
            ResourcePackProfile.InsertionPosition.TOP,
            ModResourcePackCreator.RESOURCE_PACK_SOURCE
        )?.let { consumer.accept(it) }
    }
}