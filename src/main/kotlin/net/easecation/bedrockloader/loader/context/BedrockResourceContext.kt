package net.easecation.bedrockloader.loader.context

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.BedrockTexturePath
import net.easecation.bedrockloader.bedrock.JavaTexturePath
import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.*
import net.minecraft.util.Identifier

class BedrockResourceContext {

    val terrainTexture: MutableMap<String, TextureDataDefinition> = mutableMapOf()
    val itemTexture: MutableMap<String, TextureDataDefinition> = mutableMapOf()
    val blocks: MutableMap<Identifier, BlockResourceDefinition.Block> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityResourceDefinition.ClientEntity> = mutableMapOf()
    val geometries: MutableMap<String, GeometryDefinition.Model> = mutableMapOf()
    val renderControllers: MutableMap<String, EntityRenderControllerDefinition.RenderController> = mutableMapOf()
    val textureImages: MutableMap<BedrockTexturePath, TextureImage> = mutableMapOf()  // 去除后缀的路径(如textures/block/stone) -> TextureImage(image, type(后缀类型))
    val animations: MutableMap<String, AnimationDefinition.Animation> = mutableMapOf()  // 动画ID -> 动画数据

    fun putAll(other: BedrockResourceContext) {
        terrainTexture.putAll(other.terrainTexture)
        blocks.putAll(other.blocks)
        entities.putAll(other.entities)
        geometries.putAll(other.geometries)
        renderControllers.putAll(other.renderControllers)
        textureImages.putAll(other.textureImages)
        itemTexture.putAll(other.itemTexture)
        animations.putAll(other.animations)
    }

    fun terrainTextureToJava(namespace: String, textureKey: String) : JavaTexturePath? {
        val textures = terrainTexture[textureKey]?.textures
        val texture = textures?.firstOrNull()?.path
        if (texture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $textureKey")
            return null
        }
        return Identifier.of(
            namespace,
            "block/${texture.substringAfterLast("/")}"
        )
    }

    fun itemTextureToJava(namespace: String, textureKey: String) : JavaTexturePath? {
        val textures = itemTexture[textureKey]?.textures
        val texture = textures?.firstOrNull()?.path
        if (texture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Item texture not found: $textureKey")
            return null
        }
        return Identifier.of(
            namespace,
            "item/${texture.substringAfterLast("/")}"
        )
    }
}