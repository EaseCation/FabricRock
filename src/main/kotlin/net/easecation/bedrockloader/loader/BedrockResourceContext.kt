package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.TerrainTextureDefinition
import net.minecraft.util.Identifier

class BedrockResourceContext {

    var terrainTexture: Map<String, TerrainTextureDefinition.TextureData> = mapOf()
    var blocks: Map<Identifier, BlockResourceDefinition.Block> = mapOf()
    val textureImages: MutableMap<String, TextureImage> = mutableMapOf()

}