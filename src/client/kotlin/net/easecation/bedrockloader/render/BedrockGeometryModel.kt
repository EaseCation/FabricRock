package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.model.ModelPart
import net.easecation.bedrockloader.render.model.TexturedModelData
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.model.*
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.render.model.json.Transformation
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import java.util.function.Function
import java.util.function.Supplier


@Environment(EnvType.CLIENT)
class BedrockGeometryModel private constructor(
    private val bedrockModel: GeometryDefinition.Model,
    val materials: Map<String, BedrockMaterialInstance>,
    private val transformation: ModelTransformation,
    private val modelPart: ModelPart,
    private val blockTransformation: ComponentTransformation?
) : EntityModel<EntityDataDriven>(), UnbakedModel, BakedModel, FabricBakedModel {

    companion object {
        val MODEL_TRANSFORM_BLOCK: ModelTransformation = ModelTransformation(
            ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT,
            ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT,
            ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_LEFT,
            ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_LEFT,
            Transformation.IDENTITY,
            ModelHelper.TRANSFORM_BLOCK_GUI,
            ModelHelper.TRANSFORM_BLOCK_GROUND,
            ModelHelper.TRANSFORM_BLOCK_FIXED
        )
    }

    class Factory(private val bedrockModel: GeometryDefinition.Model) {
        private fun getTexturedModelData(bedrockModel: GeometryDefinition.Model): TexturedModelData {
            if (bedrockModel.description.texture_width == null || bedrockModel.description.texture_height == null) throw IllegalStateException("[BedrockGeometryModel] Model has no texture size")
            if (bedrockModel.bones == null) throw IllegalStateException("[BedrockGeometryModel] Model has no bones")
            BedrockRenderUtil.bedrockBonesToJavaModelData(bedrockModel.bones!!).let { modelData ->
                return TexturedModelData.of(
                    modelData,
                    bedrockModel.description.texture_width!!,
                    bedrockModel.description.texture_height!!
                )
            }
        }

        fun create(
            materials: Map<String, BedrockMaterialInstance>,
            transformation: ModelTransformation = MODEL_TRANSFORM_BLOCK
        ): BedrockGeometryModel {
            val modelPart = getTexturedModelData(bedrockModel).createModel()
            return BedrockGeometryModel(bedrockModel, materials, transformation, modelPart, null)
        }
    }

    private var defaultSprite: Sprite? = null
    private var sprites: MutableMap<String, Sprite> = mutableMapOf()
    private var mesh: Mesh? = null

    fun getModelVariant(block: BlockContext.BlockDataDriven, state: BlockState): UnbakedModel {
        val blockTransformation = block.getComponents(state).minecraftTransformation ?: return this
        return BedrockGeometryModel(bedrockModel, materials, transformation, modelPart, blockTransformation)
    }

    override fun getModelDependencies(): Collection<Identifier> {
        return emptyList() // 模型不依赖于其他模型。
    }

    override fun setParents(modelLoader: Function<Identifier, UnbakedModel>?) {
        // 与模型继承有关，我们这里还不需要使用到
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings?,
        modelId: Identifier?
    ): BakedModel {
        BedrockLoader.logger.info("Baking model... $modelId ${bedrockModel.description.identifier}")
        // 获得sprites
        materials.forEach { (key, material) ->
            sprites[key] = textureGetter.apply(material.spriteId)
        }
        defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite!!, sprites, blockTransformation)
        return this
    }

    override fun getQuads(state: BlockState?, face: Direction?, random: Random?): MutableList<BakedQuad> {
        return mutableListOf() // 不需要，因为我们使用的是 FabricBakedModel
    }

    override fun useAmbientOcclusion(): Boolean {
        return true // 环境光遮蔽：我们希望方块在有临近方块时显示阴影
    }

    override fun isBuiltin(): Boolean {
        return false
    }

    override fun hasDepth(): Boolean {
        return false
    }

    override fun isSideLit(): Boolean {
        return true
    }

    override fun getParticleSprite(): Sprite {
        return defaultSprite!!
    }

    override fun isVanillaAdapter(): Boolean {
        return false // false 以触发 FabricBakedModel 渲染
    }

    override fun emitBlockQuads(blockRenderView: BlockRenderView, blockState: BlockState, blockPos: BlockPos, supplier: Supplier<Random>, renderContext: RenderContext) {
        mesh?.outputTo(renderContext.emitter)
    }

    override fun emitItemQuads(itemStack: ItemStack, supplier: Supplier<Random>, renderContext: RenderContext) {
        mesh?.outputTo(renderContext.emitter)
    }

    override fun getTransformation(): ModelTransformation {
        return transformation
    }

    override fun getOverrides(): ModelOverrideList {
        return ModelOverrideList.EMPTY
    }

    // EntityModel methods

    override fun render(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha)
    }

    override fun setAngles(entity: EntityDataDriven?, limbAngle: Float, limbDistance: Float, animationProgress: Float, headYaw: Float, headPitch: Float) {

    }

}