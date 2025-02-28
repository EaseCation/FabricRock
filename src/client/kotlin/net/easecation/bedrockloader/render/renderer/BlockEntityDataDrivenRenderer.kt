package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
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
    private val texture: Identifier
) : BlockEntityRenderer<BlockEntityDataDriven> {

    companion object {
        fun create(
            context: BlockEntityRendererFactory.Context,
            model: BedrockGeometryModel,
            texture: Identifier
        ): BlockEntityDataDrivenRenderer {
            return BlockEntityDataDrivenRenderer(context, model, texture)
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
        val renderLayer = model.getLayer(texture) ?: return
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        matrices.push()
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
        matrices.translate(0.0, -1.5, 0.0)
        model.render(matrices, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f)
        matrices.pop()
    }
}