package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.animation.EntityAnimationManager
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis

class BlockEntityDataDrivenRenderer(
    private val context: BlockEntityRendererFactory.Context,
    private val model: BedrockGeometryModel,
    private val texture: Identifier,
    private val blockIdentifier: Identifier
) : BlockEntityRenderer<BlockEntityDataDriven> {

    companion object {
        fun create(
            context: BlockEntityRendererFactory.Context,
            model: BedrockGeometryModel,
            texture: Identifier,
            blockIdentifier: Identifier
        ): BlockEntityDataDrivenRenderer {
            return BlockEntityDataDrivenRenderer(context, model, texture, blockIdentifier)
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
        val renderLayer = model.getLayer(texture) ?: return
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)

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
        model.render(matrices, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f)
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

        // 使用固定的时间增量（1/20秒 = 1 tick）
        val deltaTime = 0.05  // 1 tick = 1/20 秒

        // 更新动画
        animManager.tick(deltaTime)

        // 应用动画变换到模型
        model.applyAnimations(animManager)
    }
}