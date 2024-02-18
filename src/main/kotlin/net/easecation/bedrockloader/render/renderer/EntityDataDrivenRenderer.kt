package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.JavaTexturePath
import net.easecation.bedrockloader.render.RenderControllerWithClientEntity
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.util.Identifier

/**
 * 实现了Java版的实体渲染器，通过RenderControllerWithClientEntity来获取实体的渲染信息
 */
class EntityDataDrivenRenderer private constructor(
        context: EntityRendererFactory.Context?,
        private val renderControllerWithClientEntity: RenderControllerWithClientEntity,
        entityModel: BedrockGeometryModel?,
        shadowRadius: Float
) : MobEntityRenderer<EntityDataDriven, BedrockGeometryModel>(context, entityModel, shadowRadius) {

    companion object {

        // 吐槽：java版是直接在这边就定下来Model了，导致没法像基岩版渲染控制器那样动态决定模型
        fun create(
                context: EntityRendererFactory.Context?,
                renderControllerWithClientEntity: RenderControllerWithClientEntity,
                shadowRadius: Float
        ): EntityDataDrivenRenderer {
            val model = renderControllerWithClientEntity.getModel()
            return EntityDataDrivenRenderer(context, renderControllerWithClientEntity, model, shadowRadius)
        }

    }

    override fun getTexture(entity: EntityDataDriven): Identifier {
        val texturesList = renderControllerWithClientEntity.getTextures(entity)
        if (texturesList.isNullOrEmpty()) {
            throw IllegalStateException("No textures found for entity ${entity.type}")
        } else if (texturesList.size > 1) {
            BedrockLoader.logger.warn("Multiple textures found for entity ${entity.type}, using the first one")
        }
        // 这边的texturesList中是基岩版的材质路径，需要转换为Java版的材质路径（补上后缀）
        return Identifier(entity.identifier.namespace, texturesList[0] + ".png")
    }

}