package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.animation.EntityAnimationManager
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.RenderLayer
//? if >=1.21.11 {
/*import net.minecraft.client.render.RenderLayers
*///?}
//? if <1.21.9 {
import net.minecraft.client.render.VertexConsumerProvider
//?}
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
//? if >=1.21.9 {
/*import net.minecraft.client.render.block.entity.state.BlockEntityRenderState
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.state.CameraRenderState
import net.minecraft.client.render.OverlayTexture
import net.minecraft.util.math.Vec3d
import net.minecraft.client.render.command.ModelCommandRenderer
*///?} elif >=1.21.5 {
/*import net.minecraft.util.math.Vec3d
*///?}

//? if >=1.21.9 {
/*/^*
 * 方块实体渲染状态 (1.21.9+)
 ^/
class BlockEntityDataDrivenRenderState : BlockEntityRenderState() {
    var block: BlockContext.BlockDataDriven? = null
    var animationManager: Any? = null
    var blockIdentifierForScale: Identifier? = null
}
*///?}

/**
 * 方块实体渲染器
 *
 * 支持基岩版材质类型：
 * - entity: 标准渲染
 * - entity_alphatest: Alpha 测试（镂空）
 * - entity_alphablend: Alpha 混合（半透明）
 * - entity_emissive: 自发光
 * - entity_emissive_alpha: 自发光 + Alpha 测试
 */
class BlockEntityDataDrivenRenderer(
    private val context: BlockEntityRendererFactory.Context,
    private val model: BedrockGeometryModel,
    private val texture: Identifier,
    private val blockIdentifier: Identifier,
    private val material: BedrockEntityMaterial
//? if >=1.21.9 {
/*) : BlockEntityRenderer<BlockEntityDataDriven, BlockEntityDataDrivenRenderState> {
*///?} else {
) : BlockEntityRenderer<BlockEntityDataDriven> {
//?}

    companion object {
        /** 全亮度值 (15, 15) = 0xF000F0 */
        private const val MAX_LIGHT = 0xF000F0

        fun create(
            context: BlockEntityRendererFactory.Context,
            model: BedrockGeometryModel,
            texture: Identifier,
            blockIdentifier: Identifier,
            material: BedrockEntityMaterial = BedrockEntityMaterial.ENTITY
        ): BlockEntityDataDrivenRenderer {
            return BlockEntityDataDrivenRenderer(context, model, texture, blockIdentifier, material)
        }
    }

    /**
     * 根据材质类型获取渲染层
     *
     * 渲染层选择逻辑：
     * - blending: 半透明混合
     * - alphaTest + disableCulling: 双面镂空（如 entity_alphatest）
     * - alphaTest: 单面镂空（如 entity_alphatest_one_sided）
     * - 默认: 实心渲染
     */
    //? if >=1.21.11 {
    /*private fun getRenderLayer(): RenderLayer {
        return when {
            material.blending -> RenderLayers.entityTranslucent(texture)
            material.alphaTest && material.disableCulling -> RenderLayers.entityCutoutNoCull(texture)
            material.alphaTest -> RenderLayers.entityCutout(texture)
            else -> RenderLayers.entitySolid(texture)
        }
    }
    *///?} else {
    private fun getRenderLayer(): RenderLayer {
        return when {
            material.blending -> RenderLayer.getEntityTranslucent(texture)
            material.alphaTest && material.disableCulling -> RenderLayer.getEntityCutoutNoCull(texture)
            material.alphaTest -> RenderLayer.getEntityCutout(texture)
            else -> RenderLayer.getEntitySolid(texture)
        }
    }
    //?}

    //? if >=1.21.9 {
    /*override fun createRenderState(): BlockEntityDataDrivenRenderState {
        return BlockEntityDataDrivenRenderState()
    }

    override fun updateRenderState(
        entity: BlockEntityDataDriven,
        state: BlockEntityDataDrivenRenderState,
        tickDelta: Float,
        cameraPos: Vec3d,
        crumblingOverlay: ModelCommandRenderer.CrumblingOverlayCommand?
    ) {
        super.updateRenderState(entity, state, tickDelta, cameraPos, crumblingOverlay)
        state.block = entity.cachedState?.block as? BlockContext.BlockDataDriven
        state.blockIdentifierForScale = blockIdentifier

        // 更新动画
        var animManager = entity.animationManager as? EntityAnimationManager
        if (animManager == null) {
            val config = BedrockAddonsRegistryClient.blockEntityAnimationConfigs[blockIdentifier]
            if (config != null) {
                animManager = EntityAnimationManager(
                    config.animationMap,
                    config.animations,
                    config.autoPlayList
                )
                entity.animationManager = animManager
            }
        }
        animManager?.tick()
        animManager?.let { model.applyAnimations(it) }
        state.animationManager = animManager
    }

    override fun render(
        state: BlockEntityDataDrivenRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraRenderState: CameraRenderState
    ) {
        val blockState = state.blockState ?: return
        val block = state.block ?: return
        val renderLayer = getRenderLayer()
        val light = if (material.emissive) MAX_LIGHT else state.lightmapCoordinates
        val overlay = OverlayTexture.DEFAULT_UV

        matrices.push()

        // 应用缩放
        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))

        queue.submitCustom(matrices, renderLayer) { matrixEntry, vertexConsumer ->
            // 创建临时MatrixStack包含当前变换
            val tmpMatrices = MatrixStack()
            tmpMatrices.peek().copy(matrixEntry)
            model.renderCustom(tmpMatrices, vertexConsumer, light, overlay, -1)
        }

        matrices.pop()
    }
    *///?} elif >=1.21.5 {
    /*override fun render(
        entity: BlockEntityDataDriven,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int,
        cameraPos: Vec3d
    ) {
        val blockState = entity.cachedState ?: return
        val block = blockState.block as? BlockContext.BlockDataDriven ?: return
        val renderLayer = getRenderLayer()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        val effectiveLight = if (material.emissive) MAX_LIGHT else light
        updateAnimations(entity)

        matrices.push()

        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        model.renderCustom(matrices, vertexConsumer, effectiveLight, overlay, -1)
        matrices.pop()
    }
    *///?} elif >=1.21.2 {
    /*override fun render(
        entity: BlockEntityDataDriven,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val blockState = entity.cachedState ?: return
        val block = blockState.block as? BlockContext.BlockDataDriven ?: return
        val renderLayer = getRenderLayer()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        val effectiveLight = if (material.emissive) MAX_LIGHT else light
        updateAnimations(entity)

        matrices.push()

        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        model.renderCustom(matrices, vertexConsumer, effectiveLight, overlay, -1)
        matrices.pop()
    }
    *///?} elif >=1.21 && <1.21.2 {
    /*override fun render(
        entity: BlockEntityDataDriven,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val blockState = entity.cachedState ?: return
        val block = blockState.block as? BlockContext.BlockDataDriven ?: return
        val renderLayer = getRenderLayer()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        val effectiveLight = if (material.emissive) MAX_LIGHT else light
        updateAnimations(entity)

        matrices.push()

        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        model.render(matrices, vertexConsumer, effectiveLight, overlay, -1)
        matrices.pop()
    }
    *///?} else {
    override fun render(
        entity: BlockEntityDataDriven,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val blockState = entity.cachedState ?: return
        val block = blockState.block as? BlockContext.BlockDataDriven ?: return
        val renderLayer = getRenderLayer()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        val effectiveLight = if (material.emissive) MAX_LIGHT else light
        updateAnimations(entity)

        matrices.push()

        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        model.render(matrices, vertexConsumer, effectiveLight, overlay, 1.0f, 1.0f, 1.0f, 1.0f)
        matrices.pop()
    }
    //?}

    //? if <1.21.9 {
    private fun updateAnimations(entity: BlockEntityDataDriven) {
        var animManager = entity.animationManager as? EntityAnimationManager
        if (animManager == null) {
            val config = BedrockAddonsRegistryClient.blockEntityAnimationConfigs[blockIdentifier]
            if (config != null) {
                animManager = EntityAnimationManager(
                    config.animationMap,
                    config.animations,
                    config.autoPlayList
                )
                entity.animationManager = animManager
            } else {
                return
            }
        }
        animManager.tick()
        model.applyAnimations(animManager)
    }
    //?}
}
