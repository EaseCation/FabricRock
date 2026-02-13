package net.easecation.bedrockloader.render

import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.util.Identifier
import java.util.function.Function

/**
 * 包装 JsonUnbakedModel 并应用旋转的 UnbakedModel
 * 用于支持基岩版 transformation 在标准立方体模型上的应用
 *
 * @param baseModel 基础的 JsonUnbakedModel
 * @param rotation 要应用的旋转设置
 */
internal class RotatedJsonModel(
    private val baseModel: JsonUnbakedModel,
    internal val rotation: ModelBakeSettings
) : UnbakedModel {
    //? if >=1.21.4 {
    override fun resolve(resolver: net.minecraft.client.render.model.ResolvableModel.Resolver) {
        // 让基础模型先resolve
        baseModel.resolve(resolver)
    }

    override fun bake(
        textures: net.minecraft.client.render.model.ModelTextures,
        baker: Baker,
        settings: ModelBakeSettings,
        ambientOcclusion: Boolean,
        isSideLit: Boolean,
        transformation: net.minecraft.client.render.model.json.ModelTransformation
    ): BakedModel? {
        // 使用我们的旋转参数，而不是传入的 settings
        return baseModel.bake(textures, baker, rotation, ambientOcclusion, isSideLit, transformation)
    }
    //?} elif >=1.21.2 {
    /*override fun resolve(context: UnbakedModel.Resolver) {
        // 让基础模型先resolve
        baseModel.resolve(context)
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings
    ): BakedModel? {
        // 使用我们的旋转参数，而不是传入的 rotationContainer
        return baseModel.bake(baker, textureGetter, rotation)
    }
    *///?} else {
    /*override fun getModelDependencies(): Collection<Identifier> = baseModel.modelDependencies

    override fun setParents(modelLoader: Function<Identifier, UnbakedModel>?) {
        baseModel.setParents(modelLoader)
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings
    ): BakedModel? {
        // 使用我们的旋转参数，而不是传入的 rotationContainer
        return baseModel.bake(baker, textureGetter, rotation)
    }
    *///?}
}
