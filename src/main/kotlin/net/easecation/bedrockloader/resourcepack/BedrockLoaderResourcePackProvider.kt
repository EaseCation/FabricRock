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

        val info = ResourcePackInfo(
            "bedrock-loader-resource",
            Text.translatable("pack.name.bedrock-loader"),
            ResourcePackSource.BUILTIN,
            java.util.Optional.empty()
        )

        val factory = if (packsFolder.isDirectory)
            DirectoryBackedFactory(packsFolder.toPath())
        else
            ZipBackedFactory(packsFolder)

        val position = ResourcePackPosition(
            true,
            ResourcePackProfile.InsertionPosition.TOP,
            false
        )

        ResourcePackProfile.create(
            info,
            factory,
            ResourceType.CLIENT_RESOURCES,
            position
        )?.let { consumer.accept(it) }
    }
}