package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.animation.EntityAnimationManager
import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.model.ModelPart
import net.easecation.bedrockloader.render.model.TexturedModelData
//? if >=1.21.2 {
import net.easecation.bedrockloader.render.state.EntityDataDrivenRenderState
import net.minecraft.client.render.entity.state.EntityRenderState
//?}
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
//? if >=1.21.5 {
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel
//?} else {
/*import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
*///?}
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper
//? if <1.21.4 {
/*import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
*///?} else {
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter
import java.util.function.Predicate
//?}
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.model.*
//? if <1.21.4 {
/*import net.minecraft.client.render.model.json.ModelOverrideList
*///?}
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


/**
 * 存储骨骼的原始变换值
 */
@Environment(EnvType.CLIENT)
data class OriginalBoneTransform(
    val pivotX: Double,
    val pivotY: Double,
    val pivotZ: Double,
    val pitch: Double,
    val yaw: Double,
    val roll: Double
)

@Environment(EnvType.CLIENT)
class BedrockGeometryModel private constructor(
    private val bedrockModel: GeometryDefinition.Model,
    val materials: Map<String, BedrockMaterialInstance>,
    private val transformation: ModelTransformation,
    private val modelPart: ModelPart,
    private val blockTransformation: ComponentTransformation?,
    // 用于变体检测的基准信息
    private var blockIdentifier: Identifier? = null,
    private var baseMaterialsHash: Int? = null,
    // 骨骼名称到 ModelPart 的映射（用于动画）
    private val boneMap: Map<String, ModelPart> = emptyMap(),
    // 骨骼原始变换值（用于动画重置）
    private val originalBoneTransforms: Map<String, OriginalBoneTransform> = emptyMap()
//? if >=1.21.5 {
) : EntityModel<EntityDataDrivenRenderState>(createMinecraftModelPart()), BlockStateModel.UnbakedGrouped, BlockStateModel, FabricBlockStateModel {
    companion object {
        private fun createMinecraftModelPart(): net.minecraft.client.model.ModelPart {
            return net.minecraft.client.model.ModelPart(emptyList(), emptyMap())
        }
//?} elif >=1.21.4 {
/*) : EntityModel<EntityDataDrivenRenderState>(createMinecraftModelPart()), GroupableModel, BakedModel, FabricBakedModel {
    companion object {
        private fun createMinecraftModelPart(): net.minecraft.client.model.ModelPart {
            return net.minecraft.client.model.ModelPart(emptyList(), emptyMap())
        }
*///?} elif >=1.21.2 {
/*) : EntityModel<EntityDataDrivenRenderState>(createMinecraftModelPart()), UnbakedModel, BakedModel, FabricBakedModel {
    companion object {
        private fun createMinecraftModelPart(): net.minecraft.client.model.ModelPart {
            return net.minecraft.client.model.ModelPart(emptyList(), emptyMap())
        }
*///?} else {
/*) : EntityModel<EntityDataDriven>(), UnbakedModel, BakedModel, FabricBakedModel {
    companion object {
*///?}
        //? if >=1.21.5 {
        val MODEL_TRANSFORM_BLOCK: ModelTransformation = ModelTransformation.NONE
        //?} else {
        /*val MODEL_TRANSFORM_BLOCK: ModelTransformation = ModelTransformation(
            ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT,
            ModelHelper.TRANSFORM_BLOCK_3RD_PERSON_RIGHT,
            ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_LEFT,
            ModelHelper.TRANSFORM_BLOCK_1ST_PERSON_LEFT,
            Transformation.IDENTITY,
            ModelHelper.TRANSFORM_BLOCK_GUI,
            ModelHelper.TRANSFORM_BLOCK_GROUND,
            ModelHelper.TRANSFORM_BLOCK_FIXED
        )
        *///?}
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
         * 构建骨骼名称到 ModelPart 的映射，并保存原始变换值
         */
        private fun buildBoneMapAndOriginals(
            root: ModelPart,
            bones: List<GeometryDefinition.Bone>
        ): Pair<Map<String, ModelPart>, Map<String, OriginalBoneTransform>> {
            val boneMap = mutableMapOf<String, ModelPart>()
            val originalTransforms = mutableMapOf<String, OriginalBoneTransform>()

            fun traverse(parent: ModelPart, boneList: List<GeometryDefinition.Bone>) {
                for (bone in boneList) {
                    try {
                        val part = parent.getChild(bone.name)
                        boneMap[bone.name] = part
                        // 保存原始变换值
                        originalTransforms[bone.name] = OriginalBoneTransform(
                            pivotX = part.pivotX,
                            pivotY = part.pivotY,
                            pivotZ = part.pivotZ,
                            pitch = part.pitch,
                            yaw = part.yaw,
                            roll = part.roll
                        )
                        // 递归处理子骨骼
                        val childBones = bones.filter { it.parent == bone.name }
                        if (childBones.isNotEmpty()) {
                            traverse(part, childBones)
                        }
                    } catch (e: NoSuchElementException) {
                        // 骨骼不存在，跳过
                    }
                }
            }
            // 从根骨骼开始
            val rootBones = bones.filter { it.parent == null }
            traverse(root, rootBones)
            return Pair(boneMap, originalTransforms)
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
            val (boneMap, originalTransforms) = bedrockModel.bones?.let {
                buildBoneMapAndOriginals(modelPart, it)
            } ?: Pair(emptyMap(), emptyMap())
            return BedrockGeometryModel(
                bedrockModel, materials, transformation, modelPart, null,
                null, null, boneMap, originalTransforms
            )
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
            val (boneMap, originalTransforms) = bedrockModel.bones?.let {
                buildBoneMapAndOriginals(modelPart, it)
            } ?: Pair(emptyMap(), emptyMap())
            return BedrockGeometryModel(
                bedrockModel,
                materials,
                transformation,
                modelPart,
                null,
                identifier,
                materials.hashCode(),
                boneMap,
                originalTransforms
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
    //? if >=1.21.4 {
    fun getModelVariant(block: BlockContext.BlockDataDriven, state: BlockState): BedrockGeometryModel {
    //?} else {
    /*fun getModelVariant(block: BlockContext.BlockDataDriven, state: BlockState): UnbakedModel {
    *///?}
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
            baseMaterialsHash,
            boneMap,
            originalBoneTransforms
        )
    }

    //? if >=1.21.5 {
    override fun resolve(resolver: ResolvableModel.Resolver) {
        BedrockLoader.logger.info("Resolving model... ${bedrockModel.description.identifier}")
    }

    // BlockStateModel.UnbakedGrouped implementation
    override fun bake(state: BlockState, baker: Baker): BlockStateModel {
        BedrockLoader.logger.info("Baking model (BlockStateModel.UnbakedGrouped)... ${bedrockModel.description.identifier}")
        val spriteGetter = baker.getSpriteGetter()
        val model = object : SimpleModel { override fun name() = bedrockModel.description.identifier ?: "bedrock_geometry" }
        materials.forEach { (key, material) ->
            sprites[key] = spriteGetter.get(material.spriteId, model)
        }
        defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite!!, sprites, blockTransformation)
        return this
    }

    override fun getEqualityGroup(state: BlockState): Any = this
    //?} elif >=1.21.4 {
    /*override fun resolve(resolver: ResolvableModel.Resolver) {
        BedrockLoader.logger.info("Resolving model... ${bedrockModel.description.identifier}")
    }

    override fun bake(baker: Baker): BakedModel {
        BedrockLoader.logger.info("Baking model (GroupableModel)... ${bedrockModel.description.identifier}")
        val spriteGetter = baker.getSpriteGetter()
        materials.forEach { (key, material) ->
            sprites[key] = spriteGetter.get(material.spriteId)
        }
        defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite!!, sprites, blockTransformation)
        return this
    }

    override fun getEqualityGroup(state: BlockState): Any = this
    *///?} elif >=1.21.2 {
    /*override fun resolve(context: UnbakedModel.Resolver) {
        BedrockLoader.logger.info("Resolving model... ${bedrockModel.description.identifier}")
        // resolve方法用于预加载依赖，我们在bake时加载sprites
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings
    ): BakedModel {
        BedrockLoader.logger.info("Baking model... ${bedrockModel.description.identifier}")
        // 获得sprites
        materials.forEach { (key, material) ->
            sprites[key] = textureGetter.apply(material.spriteId)
        }
        defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite!!, sprites, blockTransformation)
        return this
    }
    *///?} else {
    /*override fun getModelDependencies(): Collection<Identifier> {
        return emptyList() // 模型不依赖于其他模型。
    }

    override fun setParents(modelLoader: Function<Identifier, UnbakedModel>?) {
        // 与模型继承有关，我们这里还不需要使用到
    }

    override fun bake(
        baker: Baker,
        textureGetter: Function<SpriteIdentifier, Sprite>,
        rotationContainer: ModelBakeSettings
    ): BakedModel {
        BedrockLoader.logger.info("Baking model... ${bedrockModel.description.identifier}")
        // 获得sprites
        materials.forEach { (key, material) ->
            sprites[key] = textureGetter.apply(material.spriteId)
        }
        defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
        mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite!!, sprites, blockTransformation)
        return this
    }
    *///?}

    //? if >=1.21.5 {
    fun getModelPartForBaking(): ModelPart = modelPart
    //?}

    //? if >=1.21.5 {
    // BlockStateModel implementation
    override fun addParts(random: Random, parts: MutableList<BlockModelPart>) {
        // Empty - we use FRAPI for rendering
    }

    override fun particleSprite(): Sprite = defaultSprite!!

    // FabricBlockStateModel implementation
    override fun emitQuads(
        emitter: QuadEmitter,
        blockView: BlockRenderView,
        pos: BlockPos,
        state: BlockState,
        random: Random,
        cullTest: Predicate<Direction?>
    ) {
        mesh?.outputTo(emitter)
    }
    //?} elif >=1.21.4 {
    /*override fun getQuads(state: BlockState?, face: Direction?, random: Random?): MutableList<BakedQuad> = mutableListOf()
    override fun useAmbientOcclusion(): Boolean = true
    override fun hasDepth(): Boolean = false
    override fun isSideLit(): Boolean = true
    override fun getParticleSprite(): Sprite = defaultSprite!!
    override fun isVanillaAdapter(): Boolean = false
    override fun emitBlockQuads(
        emitter: QuadEmitter,
        blockView: BlockRenderView,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<Random>,
        cullTest: Predicate<Direction?>
    ) { mesh?.outputTo(emitter) }
    override fun emitItemQuads(emitter: QuadEmitter, randomSupplier: Supplier<Random>) { mesh?.outputTo(emitter) }
    override fun getTransformation(): ModelTransformation = transformation
    *///?} else {
    /*override fun getQuads(state: BlockState?, face: Direction?, random: Random?): MutableList<BakedQuad> = mutableListOf()
    override fun useAmbientOcclusion(): Boolean = true
    override fun isBuiltin(): Boolean = false
    override fun hasDepth(): Boolean = false
    override fun isSideLit(): Boolean = true
    override fun getParticleSprite(): Sprite = defaultSprite!!
    override fun isVanillaAdapter(): Boolean = false
    override fun emitBlockQuads(blockRenderView: BlockRenderView, blockState: BlockState, blockPos: BlockPos, supplier: Supplier<Random>, renderContext: RenderContext) { mesh?.outputTo(renderContext.emitter) }
    override fun emitItemQuads(itemStack: ItemStack, supplier: Supplier<Random>, renderContext: RenderContext) { mesh?.outputTo(renderContext.emitter) }
    override fun getTransformation(): ModelTransformation = transformation
    override fun getOverrides(): ModelOverrideList = ModelOverrideList.EMPTY
    *///?}

    // EntityModel methods

    //? if <1.21.2 {
    /*override fun render(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, color: Int) {
        val alpha = ((color shr 24) and 0xFF) / 255.0f
        val red = ((color shr 16) and 0xFF) / 255.0f
        val green = ((color shr 8) and 0xFF) / 255.0f
        val blue = (color and 0xFF) / 255.0f
        // 使用我们自定义的ModelPart进行渲染
        modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha)
    }
    *///?}

    //? if >=1.21.2 {
    /**
     * 在1.21.2+中，render()方法是final的，不能被override
     * 我们需要通过其他方式来渲染自定义的ModelPart
     * 这个方法供外部调用来渲染模型
     */
    fun renderCustom(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, color: Int) {
        val alpha = ((color shr 24) and 0xFF) / 255.0f
        val red = ((color shr 16) and 0xFF) / 255.0f
        val green = ((color shr 8) and 0xFF) / 255.0f
        val blue = (color and 0xFF) / 255.0f
        // 使用我们自定义的ModelPart进行渲染
        modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha)
    }
    //?}

    /**
     * 应用动画变换到骨骼
     *
     * 这个方法用于方块实体渲染器，在渲染前更新骨骼状态。
     * 调用此方法前，需要先调用 animManager.tick() 更新动画状态。
     *
     * @param animManager 动画管理器
     */
    fun applyAnimations(animManager: EntityAnimationManager) {
        // 重置所有骨骼到原始状态
        for ((boneName, bone) in boneMap) {
            val original = originalBoneTransforms[boneName]
            if (original != null) {
                bone.pivotX = original.pivotX
                bone.pivotY = original.pivotY
                bone.pivotZ = original.pivotZ
                bone.pitch = original.pitch
                bone.yaw = original.yaw
                bone.roll = original.roll
            } else {
                bone.pitch = 0.0
                bone.yaw = 0.0
                bone.roll = 0.0
            }
            bone.resetScale()
            bone.resetAnimOffset()  // 重置动画位移
        }

        // 应用骨骼变换
        for ((boneName, bone) in boneMap) {
            val transform = animManager.getBoneTransform(boneName) ?: continue
            val original = originalBoneTransforms[boneName]

            // 应用旋转（基岩版角度单位是度，需要转换为弧度）
            // 动画旋转是相对于原始旋转的增量
            transform.rotation?.let { rot ->
                if (rot.size >= 3) {
                    val originalPitch = original?.pitch ?: 0.0
                    val originalYaw = original?.yaw ?: 0.0
                    val originalRoll = original?.roll ?: 0.0
                    bone.pitch = originalPitch + Math.toRadians(rot[0])
                    bone.yaw = originalYaw + Math.toRadians(rot[1])
                    bone.roll = originalRoll + Math.toRadians(rot[2])
                }
            }

            // 应用位移（使用animOffset，不修改pivot）
            // 基岩版position动画是相对于父骨骼的位移，pivot保持不变用于旋转中心
            transform.position?.let { pos ->
                if (pos.size >= 3) {
                    bone.setAnimOffset(pos[0], -pos[1], pos[2])
                }
            }

            // 应用缩放
            transform.scale?.let { scale ->
                if (scale.size >= 3) {
                    bone.setScale(scale[0], scale[1], scale[2])
                } else if (scale.size == 1) {
                    // 统一缩放
                    bone.setScale(scale[0], scale[0], scale[0])
                }
            }
        }
    }

    //? if >=1.21.2 {
    override fun setAngles(state: EntityDataDrivenRenderState) {
        // 从state中获取动画管理器
        val animManager = state.animationManager as? EntityAnimationManager
        if (animManager == null) {
            // 没有动画管理器，跳过动画处理
            return
        }

        // 更新动画（EntityAnimationManager 内部计算真实帧间隔）
        animManager.tick()

        // 重置所有骨骼到原始状态
        for ((boneName, bone) in boneMap) {
            val original = originalBoneTransforms[boneName]
            if (original != null) {
                bone.pivotX = original.pivotX
                bone.pivotY = original.pivotY
                bone.pivotZ = original.pivotZ
                bone.pitch = original.pitch
                bone.yaw = original.yaw
                bone.roll = original.roll
            } else {
                bone.pitch = 0.0
                bone.yaw = 0.0
                bone.roll = 0.0
            }
            bone.resetScale()
            bone.resetAnimOffset()  // 重置动画位移
        }

        // 应用骨骼变换
        for ((boneName, bone) in boneMap) {
            val transform = animManager.getBoneTransform(boneName) ?: continue
            val original = originalBoneTransforms[boneName]

            // 应用旋转（基岩版角度单位是度，需要转换为弧度）
            // 动画旋转是相对于原始旋转的增量
            transform.rotation?.let { rot ->
                if (rot.size >= 3) {
                    val originalPitch = original?.pitch ?: 0.0
                    val originalYaw = original?.yaw ?: 0.0
                    val originalRoll = original?.roll ?: 0.0
                    bone.pitch = originalPitch + Math.toRadians(rot[0])
                    bone.yaw = originalYaw + Math.toRadians(rot[1])
                    bone.roll = originalRoll + Math.toRadians(rot[2])
                }
            }

            // 应用位移（使用animOffset，不修改pivot）
            // 基岩版position动画是相对于父骨骼的位移，pivot保持不变用于旋转中心
            transform.position?.let { pos ->
                if (pos.size >= 3) {
                    bone.setAnimOffset(pos[0], -pos[1], pos[2])
                }
            }

            // 应用缩放
            transform.scale?.let { scale ->
                if (scale.size >= 3) {
                    bone.setScale(scale[0], scale[1], scale[2])
                } else if (scale.size == 1) {
                    // 统一缩放
                    bone.setScale(scale[0], scale[0], scale[0])
                }
            }
        }
    }
    //?} else {
    /*override fun setAngles(entity: EntityDataDriven?, limbAngle: Float, limbDistance: Float, animationProgress: Float, headYaw: Float, headPitch: Float) {
        if (entity == null) return

        // 懒加载创建动画管理器
        var animManager = entity.animationManager as? EntityAnimationManager
        if (animManager == null) {
            // 尝试从注册表获取动画配置并创建动画管理器
            val config = BedrockAddonsRegistryClient.entityAnimationConfigs[entity.identifier]
            if (config != null) {
                animManager = EntityAnimationManager(
                    config.animationMap,
                    config.animations,
                    config.autoPlayList
                )
                entity.animationManager = animManager
                BedrockLoader.logger.debug("[BedrockGeometryModel] Created animation manager for entity ${entity.identifier}")
            } else {
                // 没有动画配置，跳过动画处理
                return
            }
        }

        // 更新动画（EntityAnimationManager 内部计算真实帧间隔）
        animManager.tick()

        // 重置所有骨骼到原始状态
        for ((boneName, bone) in boneMap) {
            val original = originalBoneTransforms[boneName]
            if (original != null) {
                bone.pivotX = original.pivotX
                bone.pivotY = original.pivotY
                bone.pivotZ = original.pivotZ
                bone.pitch = original.pitch
                bone.yaw = original.yaw
                bone.roll = original.roll
            } else {
                bone.pitch = 0.0
                bone.yaw = 0.0
                bone.roll = 0.0
            }
            bone.resetScale()
            bone.resetAnimOffset()  // 重置动画位移
        }

        // 应用骨骼变换
        for ((boneName, bone) in boneMap) {
            val transform = animManager.getBoneTransform(boneName) ?: continue
            val original = originalBoneTransforms[boneName]

            // 应用旋转（基岩版角度单位是度，需要转换为弧度）
            // 动画旋转是相对于原始旋转的增量
            transform.rotation?.let { rot ->
                if (rot.size >= 3) {
                    val originalPitch = original?.pitch ?: 0.0
                    val originalYaw = original?.yaw ?: 0.0
                    val originalRoll = original?.roll ?: 0.0
                    bone.pitch = originalPitch + Math.toRadians(rot[0])
                    bone.yaw = originalYaw + Math.toRadians(rot[1])
                    bone.roll = originalRoll + Math.toRadians(rot[2])
                }
            }

            // 应用位移（使用animOffset，不修改pivot）
            // 基岩版position动画是相对于父骨骼的位移，pivot保持不变用于旋转中心
            transform.position?.let { pos ->
                if (pos.size >= 3) {
                    bone.setAnimOffset(pos[0], -pos[1], pos[2])
                }
            }

            // 应用缩放
            transform.scale?.let { scale ->
                if (scale.size >= 3) {
                    bone.setScale(scale[0], scale[1], scale[2])
                } else if (scale.size == 1) {
                    // 统一缩放
                    bone.setScale(scale[0], scale[0], scale[0])
                }
            }
        }
    }
    *///?}

}