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
    private val rotation: ModelBakeSettings
) : UnbakedModel {
    override fun getModelDependencies(): Collection<Identifier> = baseModel.modelDependencies

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
}
