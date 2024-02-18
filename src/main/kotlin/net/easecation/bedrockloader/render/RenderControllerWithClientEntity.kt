package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.BedrockTexturePath
import net.easecation.bedrockloader.bedrock.definition.EntityRenderControllerDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry

/**
 * This is a combination of the bedrock RenderController and the bedrock ClientEntity render controller instance
 * (because the render controller needs to be combined with the client entity to get the final rendered model and texture)
 * 这是一个结合了基岩版的 渲染控制器 和 客户端实体 的动画控制器实例（因为渲染控制器需要结合客户端实体才能得到最终渲染的模型和材质）
 * @param renderController 渲染控制器
 * @param clientEntity 客户端实体
 */
class RenderControllerWithClientEntity(
        private val renderController: EntityRenderControllerDefinition.RenderController,
        private val clientEntity: EntityResourceDefinition.ClientEntityDescription
) {

    fun getModel() : BedrockGeometryModel? {
        val rawGeometry = renderController.geometry
        if (rawGeometry.startsWith("Geometry.") || rawGeometry.startsWith("geometry.")) {
            val geo = rawGeometry.substring(9)
            clientEntity.geometry?.get(geo)?.let { geometry ->
                return BedrockAddonsRegistry.models[geometry]
            }
        }
        return null
    }

    fun getTextures(entity: EntityDataDriven) : List<BedrockTexturePath>? {
        // TODO 通过molang，结合entity中的数据来动态获取材质
        return renderController.textures?.map {
            if (it.startsWith("Texture.") || it.startsWith("texture.")) {
                val texture = it.substring(8)
                clientEntity.textures?.get(texture)
            } else {
                null
            }
        }?.filterNotNull()
    }

}