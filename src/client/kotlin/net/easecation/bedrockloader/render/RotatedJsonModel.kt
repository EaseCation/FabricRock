package net.easecation.bedrockloader.render

// RotatedJsonModel is not used in 1.21.5+ (block models use ModelVariant directly)
//? if >=1.21.4 && <1.21.5 {
/*import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.JsonUnbakedModel

internal class RotatedJsonModel(
    private val baseModel: JsonUnbakedModel,
    internal val rotation: ModelBakeSettings
) : UnbakedModel {
    override fun resolve(resolver: net.minecraft.client.render.model.ResolvableModel.Resolver) {
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
        return baseModel.bake(textures, baker, rotation, ambientOcclusion, isSideLit, transformation)
    }
}
*///?} elif >=1.21.2 && <1.21.4 {
/*import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import java.util.function.Function

internal class RotatedJsonModel(
    private val baseModel: JsonUnbakedModel,
    internal val rotation: ModelBakeSettings
) : UnbakedModel {
    override fun resolve(context: UnbakedModel.Resolver) {
        baseModel.resolve(context)
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings
    ): BakedModel? {
        return baseModel.bake(baker, textureGetter, rotation)
    }
}
*///?} elif >=1.21 && <1.21.2 {
/*import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.util.Identifier
import java.util.function.Function

internal class RotatedJsonModel(
    private val baseModel: JsonUnbakedModel,
    internal val rotation: ModelBakeSettings
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
        return baseModel.bake(baker, textureGetter, rotation)
    }
}
*///?} elif <1.21 {
/*import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.util.Identifier
import java.util.function.Function

internal class RotatedJsonModel(
    private val baseModel: JsonUnbakedModel,
    internal val rotation: ModelBakeSettings
) : UnbakedModel {
    override fun getModelDependencies(): Collection<Identifier> = baseModel.modelDependencies

    override fun setParents(modelLoader: Function<Identifier, UnbakedModel>?) {
        baseModel.setParents(modelLoader)
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings,
        modelId: Identifier
    ): BakedModel? {
        return baseModel.bake(baker, textureGetter, rotation, modelId)
    }
}
*///?}
