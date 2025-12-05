package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier

/**
 * 实现了Java版的实体渲染器，通过RenderControllerWithClientEntity来获取实体的渲染信息
 *
 * 支持基岩版材质类型：
 * - entity: 标准渲染
 * - entity_alphatest: Alpha 测试（镂空）
 * - entity_alphablend: Alpha 混合（半透明）
 * - entity_emissive: 自发光
 * - entity_emissive_alpha: 自发光 + Alpha 测试
 */
class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context,
    entityModel: EntityModel<EntityDataDriven>,
    shadowRadius: Float,
    private val texture: Identifier,
    private val entityIdentifier: Identifier,
    private val material: BedrockEntityMaterial
) : MobEntityRenderer<EntityDataDriven, EntityModel<EntityDataDriven>>(context, entityModel, shadowRadius) {

    companion object {
        /** 全亮度值 (15, 15) = 0xF000F0 */
        private const val MAX_LIGHT = 0xF000F0

        fun create(
            context: EntityRendererFactory.Context,
            model: EntityModel<EntityDataDriven>,
            shadowRadius: Float,
            texture: Identifier,
            entityIdentifier: Identifier,
            material: BedrockEntityMaterial = BedrockEntityMaterial.ENTITY
        ): EntityDataDrivenRenderer {
            return EntityDataDrivenRenderer(context, model, shadowRadius, texture, entityIdentifier, material)
        }
    }

    override fun getTexture(entity: EntityDataDriven): Identifier {
        return texture
    }

    /**
     * 根据材质类型选择渲染层
     *
     * 渲染层选择逻辑：
     * - blending: 半透明混合
     * - alphaTest + disableCulling: 双面镂空（如 entity_alphatest）
     * - alphaTest: 单面镂空（如 entity_alphatest_one_sided）
     * - 默认: 实心渲染
     */
    override fun getRenderLayer(
        entity: EntityDataDriven,
        showBody: Boolean,
        translucent: Boolean,
        showOutline: Boolean
    ): RenderLayer? {
        val texture = getTexture(entity)
        return when {
            !showBody -> null
            translucent -> RenderLayer.getItemEntityTranslucentCull(texture)
            showOutline -> RenderLayer.getOutline(texture)
            material.blending -> RenderLayer.getEntityTranslucent(texture)
            material.alphaTest && material.disableCulling -> RenderLayer.getEntityCutoutNoCull(texture)
            material.alphaTest -> RenderLayer.getEntityCutout(texture)
            else -> RenderLayer.getEntitySolid(texture)
        }
    }

    /**
     * 重写渲染方法，支持自发光材质
     */
    override fun render(
        entity: EntityDataDriven,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        // 如果材质是自发光类型，使用全亮度
        val effectiveLight = if (material.emissive) MAX_LIGHT else light
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, effectiveLight)
    }

    /**
     * 重写 scale 方法，应用配置的缩放值到 MatrixStack
     */
    override fun scale(entity: EntityDataDriven, matrices: MatrixStack, amount: Float) {
        val scale = BedrockAddonsRegistryClient.entityScaleConfigs[entityIdentifier] ?: 1.0f
        matrices.scale(scale, scale, scale)
    }

}
