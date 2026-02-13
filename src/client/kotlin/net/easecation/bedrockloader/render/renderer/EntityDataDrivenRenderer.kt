package net.easecation.bedrockloader.render.renderer

import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.entity.LivingEntityRenderer
//? if >=1.21.2 {
import net.easecation.bedrockloader.render.state.EntityDataDrivenRenderState
import net.easecation.bedrockloader.animation.EntityAnimationManager
//?}
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.MobEntityRenderer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

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
//? if >=1.21.2 {
class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context,
    entityModel: EntityModel<EntityDataDrivenRenderState>,
    shadowRadius: Float,
    private val texture: Identifier,
    private val entityIdentifier: Identifier,
    private val material: BedrockEntityMaterial
) : MobEntityRenderer<EntityDataDriven, EntityDataDrivenRenderState, EntityModel<EntityDataDrivenRenderState>>(context, entityModel, shadowRadius) {
//?} else {
/*class EntityDataDrivenRenderer private constructor(
    context: EntityRendererFactory.Context,
    entityModel: EntityModel<EntityDataDriven>,
    shadowRadius: Float,
    private val texture: Identifier,
    private val entityIdentifier: Identifier,
    private val material: BedrockEntityMaterial
) : MobEntityRenderer<EntityDataDriven, EntityModel<EntityDataDriven>>(context, entityModel, shadowRadius) {
*///?}

    companion object {
        /** 全亮度值 (15, 15) = 0xF000F0 */
        private const val MAX_LIGHT = 0xF000F0

        //? if >=1.21.2 {
        fun create(
            context: EntityRendererFactory.Context,
            model: EntityModel<EntityDataDrivenRenderState>,
            shadowRadius: Float,
            texture: Identifier,
            entityIdentifier: Identifier,
            material: BedrockEntityMaterial = BedrockEntityMaterial.ENTITY
        ): EntityDataDrivenRenderer {
            return EntityDataDrivenRenderer(context, model, shadowRadius, texture, entityIdentifier, material)
        }
        //?} else {
        /*fun create(
            context: EntityRendererFactory.Context,
            model: EntityModel<EntityDataDriven>,
            shadowRadius: Float,
            texture: Identifier,
            entityIdentifier: Identifier,
            material: BedrockEntityMaterial = BedrockEntityMaterial.ENTITY
        ): EntityDataDrivenRenderer {
            return EntityDataDrivenRenderer(context, model, shadowRadius, texture, entityIdentifier, material)
        }
        *///?}
    }

    //? if >=1.21.2 {
    override fun createRenderState(): EntityDataDrivenRenderState {
        return EntityDataDrivenRenderState()
    }

    override fun updateRenderState(
        entity: EntityDataDriven,
        state: EntityDataDrivenRenderState,
        tickDelta: Float
    ) {
        super.updateRenderState(entity, state, tickDelta)
        // 将entity数据复制到state
        state.identifier = entity.identifier
        state.scale = BedrockAddonsRegistryClient.entityScaleConfigs[entityIdentifier] ?: 1.0f

        // 懒加载创建动画管理器
        var animManager = entity.animationManager as? EntityAnimationManager
        if (animManager == null) {
            // 尝试从注册表获取动画配置并创建动画管理器
            val config = BedrockAddonsRegistryClient.entityAnimationConfigs[entity.identifier]
            if (config != null) {
                animManager = EntityAnimationManager(
                    config.animationMap,
                    config.animations,
                    config.autoPlayList
                )
                entity.animationManager = animManager
            }
        }
        state.animationManager = animManager
    }
    //?}

    //? if >=1.21.2 {
    override fun getTexture(state: EntityDataDrivenRenderState): Identifier {
        return texture
    }
    //?} else {
    /*override fun getTexture(entity: EntityDataDriven): Identifier {
        return texture
    }
    *///?}

    /**
     * 根据材质类型选择渲染层
     *
     * 渲染层选择逻辑：
     * - blending: 半透明混合
     * - alphaTest + disableCulling: 双面镂空（如 entity_alphatest）
     * - alphaTest: 单面镂空（如 entity_alphatest_one_sided）
     * - 默认: 实心渲染
     */
    //? if >=1.21.2 {
    override fun getRenderLayer(
        state: EntityDataDrivenRenderState,
        showBody: Boolean,
        translucent: Boolean,
        showOutline: Boolean
    ): RenderLayer? {
        val texture = getTexture(state)
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
    //?} else {
    /*override fun getRenderLayer(
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
    *///?}

    /**
     * 重写渲染方法，支持自发光材质
     */
    //? if >=1.21.2 {
    override fun render(
        state: EntityDataDrivenRenderState,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val effectiveLight = if (material.emissive) MAX_LIGHT else light

        val model = this.model
        if (model is BedrockGeometryModel) {
            matrices.push()

            // 复制 LivingEntityRenderer.render() 的变换管道
            // 注意：不使用原版的 translate(0, -1.501F, 0)，因为基岩版模型脚部在 model Y=0，
            // 而原版 Java 模型脚部在 model Y≈24（1.5格），-1.501F 偏移仅适用于原版坐标系
            this.setupTransforms(state, matrices, state.bodyYaw, state.baseScale)
            matrices.scale(-1.0F, -1.0F, 1.0F)
            this.scale(state, matrices)

            // 设置模型动画角度
            model.setAngles(state)

            // 正确计算 overlay：受伤时显示红色，受击时白色闪烁
            val overlay = LivingEntityRenderer.getOverlay(state, this.getAnimationCounter(state))

            // 正确传递隐身和发光状态到渲染层
            val renderLayer = this.getRenderLayer(state, true, state.invisibleToPlayer, state.hasOutline)
            if (renderLayer != null) {
                val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
                model.renderCustom(matrices, vertexConsumer, effectiveLight, overlay, -1)
            }

            matrices.pop()

            // 名牌渲染 (来自 EntityRenderer.render())
            if (state.displayName != null) {
                this.renderLabelIfPresent(state, state.displayName, matrices, vertexConsumers, light)
            }
        } else {
            super.render(state, matrices, vertexConsumers, effectiveLight)
        }
    }
    //?} else {
    /*override fun render(
        entity: EntityDataDriven,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        val effectiveLight = if (material.emissive) MAX_LIGHT else light

        val model = this.model
        if (model is BedrockGeometryModel) {
            matrices.push()

            // 复制 LivingEntityRenderer.render() 的变换管道
            // 注意：不使用原版的 translate(0, -1.501F, 0)，因为基岩版模型脚部在 model Y=0，
            // 而原版 Java 模型脚部在 model Y≈24（1.5格），-1.501F 偏移仅适用于原版坐标系
            val bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw)
            val animationProgress = this.getAnimationProgress(entity, tickDelta)
            this.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, entity.scale)
            matrices.scale(-1.0F, -1.0F, 1.0F)
            this.scale(entity, matrices, tickDelta)

            // 计算肢体动画参数
            var limbSpeed = 0.0F
            var limbPos = 0.0F
            if (!entity.hasVehicle() && entity.isAlive) {
                limbSpeed = entity.limbAnimator.getSpeed(tickDelta)
                limbPos = entity.limbAnimator.getPos(tickDelta)
                if (limbSpeed > 1.0F) {
                    limbSpeed = 1.0F
                }
            }

            // 设置模型动画角度
            val headYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevHeadYaw, entity.headYaw) - bodyYaw
            val headPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.pitch)
            model.animateModel(entity, limbPos, limbSpeed, tickDelta)
            model.setAngles(entity, limbPos, limbSpeed, animationProgress, headYaw, headPitch)

            // 计算 overlay 和渲染
            val overlay = LivingEntityRenderer.getOverlay(entity, this.getAnimationCounter(entity, tickDelta))
            val client = MinecraftClient.getInstance()
            val visible = this.isVisible(entity)
            val translucent = !visible && !entity.isInvisibleTo(client.player)
            val hasOutline = client.hasOutline(entity)
            val renderLayer = this.getRenderLayer(entity, visible, translucent, hasOutline)
            if (renderLayer != null) {
                val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
                val color = if (translucent) 654311423 else -1
                model.render(matrices, vertexConsumer, effectiveLight, overlay, color)
            }

            matrices.pop()

            // 名牌渲染 (来自 EntityRenderer.render())
            if (this.hasLabel(entity)) {
                this.renderLabelIfPresent(entity, entity.displayName, matrices, vertexConsumers, effectiveLight, tickDelta)
            }
        } else {
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, effectiveLight)
        }
    }
    *///?}

    /**
     * 重写 scale 方法，应用配置的缩放值到 MatrixStack
     */
    //? if >=1.21.2 {
    override fun scale(state: EntityDataDrivenRenderState, matrices: MatrixStack) {
        val scale = state.scale
        matrices.scale(scale, scale, scale)
    }
    //?} else {
    /*override fun scale(entity: EntityDataDriven, matrices: MatrixStack, amount: Float) {
        val scale = BedrockAddonsRegistryClient.entityScaleConfigs[entityIdentifier] ?: 1.0f
        matrices.scale(scale, scale, scale)
    }
    *///?}

}
