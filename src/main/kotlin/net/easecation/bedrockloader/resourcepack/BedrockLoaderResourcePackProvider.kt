package net.easecation.bedrockloader.resourcepack

import net.easecation.bedrockloader.BedrockLoader
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator
import net.minecraft.resource.*
import java.io.File
import java.util.function.Consumer
import java.util.function.Supplier

class BedrockLoaderResourcePackProvider : ResourcePackProvider {

    private val packsFolder: File = BedrockLoader.getTmpResourceDir()

    override fun register(profileAdder: Consumer<ResourcePackProfile>, factory: ResourcePackProfile.Factory) {
        if (!packsFolder.isDirectory) {
            packsFolder.mkdir()
        }

        val resourcePackProfile = ResourcePackProfile.of(
                "Bedrock Loader",
                true,
                this.createResourcePack(packsFolder),
                factory,
                ResourcePackProfile.InsertionPosition.TOP,
                ModResourcePackCreator.RESOURCE_PACK_SOURCE
        )
        if (resourcePackProfile != null) {
            profileAdder.accept(resourcePackProfile)
        }
    }

    private fun createResourcePack(file: File): Supplier<ResourcePack> {
        if (file.isDirectory) {
            return Supplier { BedrockLoaderResourcePack(file) }
        }
        return Supplier { ZipResourcePack(file) }
    }

}