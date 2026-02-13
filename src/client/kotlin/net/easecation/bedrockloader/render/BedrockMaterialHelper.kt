package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.minecraft.client.texture.MissingSprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.util.Identifier

/**
 * 基岩版材质实例辅助类
 * 提供动态创建材质映射的功能，用于支持permutations中的材质切换
 */
object BedrockMaterialHelper {

    /**
     * 从ComponentMaterialInstances创建材质映射
     *
     * @param namespace 命名空间，用于生成纹理路径
     * @param identifier 方块标识符，用于生成纹理路径
     * @param materialInstances 材质实例定义（包含纹理名称）
     * @return 材质映射（material_instance名称 -> BedrockMaterialInstance）
     */
    fun createMaterialsFromInstances(
        namespace: String,
        identifier: Identifier,
        materialInstances: ComponentMaterialInstances
    ): Map<String, BedrockMaterialInstance> {
        // 使用通配符key获取全局资源包上下文
        val context = BedrockAddonsRegistryClient.packContexts["*"]
        if (context == null) {
            BedrockLoader.logger.warn("[BedrockMaterialHelper] Global pack context not found")
            return createMissingMaterials(materialInstances)
        }

        return materialInstances.mapValues { (key, material) ->
            val textureKey = material.texture
            if (textureKey == null) {
                BedrockLoader.logger.warn("[BedrockMaterialHelper] Material instance '$key' has no texture defined for block $identifier")
                return@mapValues createMissingMaterial()
            }

            // 从terrain_texture.json查找纹理路径
            val textures = context.resource.terrainTexture[textureKey]?.textures
            if (textures == null) {
                BedrockLoader.logger.warn("[BedrockMaterialHelper] Texture '$textureKey' not found in terrain_texture.json for block $identifier")
                return@mapValues createMissingMaterial()
            }

            val texture = textures.firstOrNull()?.path
            if (texture == null) {
                BedrockLoader.logger.warn("[BedrockMaterialHelper] Texture path is null for '$textureKey' in block $identifier")
                return@mapValues createMissingMaterial()
            }

            // 创建SpriteIdentifier
            val spriteId = SpriteIdentifier(
                VersionCompat.BLOCK_ATLAS_TEXTURE,
                Identifier.of(namespace, "block/${texture.substringAfterLast("/")}")
            )

            BedrockMaterialInstance(spriteId)
        }
    }

    /**
     * 创建缺失纹理的材质实例
     */
    private fun createMissingMaterial(): BedrockMaterialInstance {
        return BedrockMaterialInstance(
            SpriteIdentifier(
                VersionCompat.BLOCK_ATLAS_TEXTURE,
                MissingSprite.getMissingSpriteId()
            )
        )
    }

    /**
     * 为所有材质实例键创建缺失纹理映射
     */
    private fun createMissingMaterials(materialInstances: ComponentMaterialInstances): Map<String, BedrockMaterialInstance> {
        return materialInstances.mapValues { createMissingMaterial() }
    }
}
