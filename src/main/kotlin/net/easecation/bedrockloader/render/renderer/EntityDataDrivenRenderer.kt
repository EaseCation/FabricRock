package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.util.Identifier

/**
 * 实现了Java版的实体渲染器，通过RenderControllerWithClientEntity来获取实体的渲染信息
 */
class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context?,
    entityModel: BedrockGeometryModel?,
    shadowRadius: Float
) : MobEntityRenderer<EntityDataDriven, BedrockGeometryModel>(context, entityModel, shadowRadius) {

    companion object {
        fun create(
            context: EntityRendererFactory.Context?,
            clientEntity: EntityResourceDefinition.ClientEntityDescription,
            shadowRadius: Float
        ): EntityDataDrivenRenderer {
            val model = BedrockAddonsRegistry.entityModel[clientEntity.identifier]
            return EntityDataDrivenRenderer(context, model, shadowRadius)
        }
    }

    override fun getTexture(entity: EntityDataDriven): Identifier {
        return model.spriteId.textureId
    }

}