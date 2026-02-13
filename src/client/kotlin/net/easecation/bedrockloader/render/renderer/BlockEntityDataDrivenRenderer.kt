package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.animation.EntityAnimationManager
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis

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
) : BlockEntityRenderer<BlockEntityDataDriven> {

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
    private fun getRenderLayer(): RenderLayer {
        return when {
            material.blending -> RenderLayer.getEntityTranslucent(texture)
            material.alphaTest && material.disableCulling -> RenderLayer.getEntityCutoutNoCull(texture)
            material.alphaTest -> RenderLayer.getEntityCutout(texture)
            else -> RenderLayer.getEntitySolid(texture)
        }
    }

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

        // 如果材质是自发光类型，使用全亮度
        val effectiveLight = if (material.emissive) MAX_LIGHT else light

        // 更新动画
        updateAnimations(entity)

        matrices.push()

        // 应用缩放
        val scale = BedrockAddonsRegistryClient.blockEntityScaleConfigs[blockIdentifier] ?: 1.0f
        if (scale != 1.0f) {
            // 移动到方块中心，缩放，再移回
            matrices.translate(0.5, 0.0, 0.5)
            matrices.scale(scale, scale, scale)
            matrices.translate(-0.5, 0.0, -0.5)
        }

        val entry = matrices.peek()
        block.applyFaceDirectional(blockState, entry.positionMatrix, entry.normalMatrix)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        //? if >=1.21.2 {
        model.renderCustom(matrices, vertexConsumer, effectiveLight, overlay, -1)
        //?} else {
        /*model.render(matrices, vertexConsumer, effectiveLight, overlay, -1)
        *///?}
        matrices.pop()
    }

    /**
     * 更新方块实体的动画状态
     *
     * 懒加载创建动画管理器，并在每帧更新动画。
     */
    private fun updateAnimations(entity: BlockEntityDataDriven) {
        // 懒加载创建动画管理器
        var animManager = entity.animationManager as? EntityAnimationManager
        if (animManager == null) {
            // 尝试从注册表获取动画配置并创建动画管理器
            val config = BedrockAddonsRegistryClient.blockEntityAnimationConfigs[blockIdentifier]
            if (config != null) {
                animManager = EntityAnimationManager(
                    config.animationMap,
                    config.animations,
                    config.autoPlayList
                )
                entity.animationManager = animManager
            } else {
                // 没有动画配置，跳过动画处理
                return
            }
        }

        // 更新动画（EntityAnimationManager 内部计算真实帧间隔）
        animManager.tick()

        // 应用动画变换到模型
        model.applyAnimations(animManager)
    }
}
