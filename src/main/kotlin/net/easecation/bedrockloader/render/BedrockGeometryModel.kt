package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.model.ModelPart
import net.easecation.bedrockloader.render.model.TexturedModelData
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.model.*
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockRenderView
import java.util.*
import java.util.function.Supplier
import kotlin.collections.ArrayList


@Environment(EnvType.CLIENT)
class BedrockGeometryModel(
        private val bedrockModel: GeometryDefinition.Model,
) : EntityModel<EntityDataDriven>(), UnbakedModel, BakedModel, FabricBakedModel {

    private val defaultBlockModel = Identifier("minecraft:block/block")
    private val spriteIds: ArrayList<SpriteIdentifier> = ArrayList()
    private val sprites: ArrayList<Sprite> = ArrayList()

    private var transformation: ModelTransformation? = null
    private val modelPart: ModelPart = getTexturedModelData().createModel()
    private var mesh: Mesh? = null

    private fun getTexturedModelData(): TexturedModelData {
        if (bedrockModel.description.texture_width == null || bedrockModel.description.texture_height == null) throw IllegalStateException("[BedrockGeometryModel] Model has no texture size")
        if (bedrockModel.bones == null) throw IllegalStateException("[BedrockGeometryModel] Model has no bones")
        BedrockRenderUtil.bedrockBonesToJavaModelData(bedrockModel.bones).let { modelData ->
            return TexturedModelData.of(modelData, bedrockModel.description.texture_width, bedrockModel.description.texture_height)
        }
    }

    fun addSprite(spriteId: SpriteIdentifier) {
        spriteIds.add(spriteId)
    }

    override fun getModelDependencies(): Collection<Identifier> {
        return emptyList() // 模型不依赖于其他模型。
    }

    override fun getTextureDependencies(unbakedModelGetter: java.util.function.Function<Identifier, UnbakedModel>?, unresolvedTextureReferences: MutableSet<com.mojang.datafixers.util.Pair<String, String>>?): MutableList<SpriteIdentifier> {
        val map = mutableListOf<SpriteIdentifier>()
        spriteIds.forEach {
            map.add(it)
        }
        return map  // 本模型（以及其模型依赖，依赖的依赖，等）依赖的纹理。 TODO
    }

    override fun bake(loader: ModelLoader, textureGetter: java.util.function.Function<SpriteIdentifier, Sprite>, rotationContainer: ModelBakeSettings?, modelId: Identifier?): BakedModel {
        BedrockLoader.logger.info("Baking model... $modelId ${bedrockModel.description.identifier}")
        // 加载默认方块模型
        val defaultBlockModel = loader.getOrLoadModel(defaultBlockModel) as JsonUnbakedModel
        // 获取 ModelTransformation
        transformation = defaultBlockModel.transformations
        // 获得sprites
        spriteIds.forEach {
            sprites.add(textureGetter.apply(it))
        }
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, sprites[0])
        return this
    }

    override fun getQuads(state: BlockState?, face: Direction?, random: Random?): List<BakedQuad> {
        return emptyList() // 不需要，因为我们使用的是 FabricBakedModel
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
        return sprites[0] // 方块被破坏时产生的颗粒，使用furnace_top
    }

    override fun isVanillaAdapter(): Boolean {
        return false // false 以触发 FabricBakedModel 渲染
    }

    override fun emitBlockQuads(blockView: BlockRenderView?, state: BlockState?, pos: BlockPos?, randomSupplier: Supplier<Random>?, context: net.fabricmc.fabric.api.renderer.v1.render.RenderContext?) {
        context?.meshConsumer()?.accept(mesh)
    }

    override fun emitItemQuads(stack: ItemStack?, randomSupplier: Supplier<Random>?, context: net.fabricmc.fabric.api.renderer.v1.render.RenderContext) {
        context.meshConsumer().accept(mesh);
    }

    override fun getTransformation(): ModelTransformation {
        return transformation!!
    }

    override fun getOverrides(): ModelOverrideList {
        return ModelOverrideList.EMPTY
    }

    // EntityModel methods

    override fun render(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        matrices.translate(0.0, 1.5, 0.0)
        modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha)
    }

    override fun setAngles(entity: EntityDataDriven?, limbAngle: Float, limbDistance: Float, animationProgress: Float, headYaw: Float, headPitch: Float) {

    }

}