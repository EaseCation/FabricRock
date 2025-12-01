package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

/**
 * 实现了Java版的实体渲染器，通过RenderControllerWithClientEntity来获取实体的渲染信息
 */
class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context,
    entityModel: EntityModel<EntityDataDriven>,
    shadowRadius: Float,
    private val texture: Identifier,
    private val entityIdentifier: Identifier
) : MobEntityRenderer<EntityDataDriven, EntityModel<EntityDataDriven>>(context, entityModel, shadowRadius) {

    companion object {
        fun create(
            context: EntityRendererFactory.Context,
            model: EntityModel<EntityDataDriven>,
            shadowRadius: Float,
            texture: Identifier,
            entityIdentifier: Identifier
        ): EntityDataDrivenRenderer {
            return EntityDataDrivenRenderer(context, model, shadowRadius, texture, entityIdentifier)
        }
    }

    override fun getTexture(entity: EntityDataDriven): Identifier {
        return texture
    }

    /**
     * 重写 scale 方法，应用配置的缩放值到 MatrixStack
     */
    override fun scale(entity: EntityDataDriven, matrices: MatrixStack, amount: Float) {
        val scale = BedrockAddonsRegistryClient.entityScaleConfigs[entityIdentifier] ?: 1.0f
        matrices.scale(scale, scale, scale)
    }

}