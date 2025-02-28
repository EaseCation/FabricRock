package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.entity.EntityDataDriven
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.util.Identifier

/**
 * 实现了Java版的实体渲染器，通过RenderControllerWithClientEntity来获取实体的渲染信息
 */
class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context,
    entityModel: EntityModel<EntityDataDriven>,
    shadowRadius: Float,
    private val texture: Identifier
) : MobEntityRenderer<EntityDataDriven, EntityModel<EntityDataDriven>>(context, entityModel, shadowRadius) {

    companion object {
        fun create(
            context: EntityRendererFactory.Context,
            model: EntityModel<EntityDataDriven>,
            shadowRadius: Float,
            texture: Identifier
        ): EntityDataDrivenRenderer {
            return EntityDataDrivenRenderer(context, model, shadowRadius, texture)
        }
    }

    override fun getTexture(entity: EntityDataDriven): Identifier {
        return texture
    }

}