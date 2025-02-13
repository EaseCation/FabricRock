package net.easecation.bedrockloader.loader.context

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.BedrockTexturePath
import net.easecation.bedrockloader.bedrock.JavaTexturePath
import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.*
import net.minecraft.util.Identifier

class BedrockResourceContext {

    val terrainTexture: MutableMap<String, TerrainTextureDefinition.TextureData> = mutableMapOf()
    val itemTexture: MutableMap<String, ItemTextureDefinition.TextureData> = mutableMapOf()
    val blocks: MutableMap<Identifier, BlockResourceDefinition.Block> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityResourceDefinition.ClientEntity> = mutableMapOf()
    val geometries: MutableMap<String, GeometryDefinition.Model> = mutableMapOf()
    val renderControllers: MutableMap<String, EntityRenderControllerDefinition.RenderController> = mutableMapOf()
    val textureImages: MutableMap<BedrockTexturePath, TextureImage> = mutableMapOf()  // 去除后缀的路径(如textures/block/stone) -> TextureImage(image, type(后缀类型))

    fun putAll(other: BedrockResourceContext) {
        terrainTexture.putAll(other.terrainTexture)
        blocks.putAll(other.blocks)
        entities.putAll(other.entities)
        geometries.putAll(other.geometries)
        renderControllers.putAll(other.renderControllers)
        textureImages.putAll(other.textureImages)
        itemTexture.putAll(other.itemTexture)
    }

    fun terrainTextureToJava(namespace: String, textureKey: String) : JavaTexturePath? {
        val texture = terrainTexture[textureKey]?.textures
        if (texture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $textureKey")
            return null
        }
        return Identifier(
            namespace,
            "block/${texture.substringAfterLast("/")}"
        )
    }

    fun itemTextureToJava(namespace: String, textureKey: String) : JavaTexturePath? {
        val texture = itemTexture[textureKey]?.textures
        if (texture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Item texture not found: $textureKey")
            return null
        }
        return Identifier(
            namespace,
            "item/${texture.substringAfterLast("/")}"
        )
    }
}