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
    private val blockTransformation: ComponentTransformation?,
    // 用于变体检测的基准信息
    private var blockIdentifier: Identifier? = null,
    private var baseMaterialsHash: Int? = null
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

        /**
         * 创建基础几何体模型
         *
         * @param materials 材质映射
         * @param transformation 模型变换
         * @return 基础几何体模型
         */
        fun create(
            materials: Map<String, BedrockMaterialInstance>,
            transformation: ModelTransformation = MODEL_TRANSFORM_BLOCK
        ): BedrockGeometryModel {
            val modelPart = getTexturedModelData(bedrockModel).createModel()
            return BedrockGeometryModel(bedrockModel, materials, transformation, modelPart, null)
        }

        /**
         * 创建支持动态材质切换的几何体模型
         *
         * @param materials 基础材质映射
         * @param identifier 方块标识符，用于后续动态创建材质
         * @param transformation 模型变换
         * @return 支持动态材质的几何体模型
         */
        fun create(
            materials: Map<String, BedrockMaterialInstance>,
            identifier: Identifier,
            transformation: ModelTransformation = MODEL_TRANSFORM_BLOCK
        ): BedrockGeometryModel {
            val modelPart = getTexturedModelData(bedrockModel).createModel()
            return BedrockGeometryModel(
                bedrockModel,
                materials,
                transformation,
                modelPart,
                null,
                identifier,
                materials.hashCode()
            )
        }
    }

    private var defaultSprite: Sprite? = null
    private var sprites: MutableMap<String, Sprite> = mutableMapOf()
    private var mesh: Mesh? = null

    /**
     * 获取特定BlockState的模型变体
     *
     * 根据BlockState的components（经过permutations烘焙），动态创建模型变体。
     * 支持以下组件的动态切换：
     * - minecraft:transformation（旋转/缩放/位移）
     * - minecraft:material_instances（材质/纹理）
     *
     * @param block 方块数据驱动实例
     * @param state 方块状态
     * @return 对应状态的UnbakedModel（可能是this本身或新创建的变体）
     */
    fun getModelVariant(block: BlockContext.BlockDataDriven, state: BlockState): UnbakedModel {
        val components = block.getComponents(state)
        val newTransformation = components.minecraftTransformation
        val newMaterialInstances = components.minecraftMaterialInstances

        // 检查是否需要创建新的材质映射
        val needsNewMaterials = blockIdentifier != null &&
                newMaterialInstances != null &&
                newMaterialInstances.hashCode() != baseMaterialsHash

        // 检查是否有transformation变化
        val hasTransformation = newTransformation != null

        // 如果都没有变化，复用基础模型
        if (!needsNewMaterials && !hasTransformation) {
            return this
        }

        // 创建新的材质映射（如果需要）
        val newMaterials = if (needsNewMaterials && blockIdentifier != null) {
            BedrockMaterialHelper.createMaterialsFromInstances(
                blockIdentifier!!.namespace,
                blockIdentifier!!,
                newMaterialInstances!!
            ).also {
                BedrockLoader.logger.debug(
                    "[BedrockGeometryModel] Created material variant for block ${blockIdentifier} state $state: ${newMaterialInstances.keys}"
                )
            }
        } else {
            materials
        }

        // 创建新的模型变体
        return BedrockGeometryModel(
            bedrockModel,
            newMaterials,
            transformation,
            modelPart,
            newTransformation,
            blockIdentifier,
            baseMaterialsHash
        )
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