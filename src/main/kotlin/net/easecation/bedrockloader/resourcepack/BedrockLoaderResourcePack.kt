package net.easecation.bedrockloader.resourcepack

import net.minecraft.resource.DirectoryResourcePack
import java.io.File

class BedrockLoaderResourcePack(file: File) : DirectoryResourcePack(file) {

    override fun getName(): String {
        return "Bedrock Loader"
    }

}