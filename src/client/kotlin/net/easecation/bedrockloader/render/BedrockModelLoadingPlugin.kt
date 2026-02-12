package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.BedrockLoaderClient
import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.render.model.ModelRotation
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.util.Identifier

object BedrockModelLoadingPlugin : ModelLoadingPlugin {
    /**
     * 将基岩版的 ComponentTransformation 转换为 Minecraft 的 ModelRotation
     *
     * 限制：
     * - 仅支持 Y 轴旋转
     * - 仅支持 90 度增量（0°/90°/180°/270°）
     * - 忽略缩放和平移
     */
    private fun transformationToModelRotation(transformation: ComponentTransformation): ModelRotation {
        val rotation = transformation.rotation

        // 提取Y轴旋转角度（rotation[1]是Y轴）
        val yRotation = if (rotation != null && rotation.size >= 2) {
            rotation[1].toInt()
        } else {
            0
        }

        // 归一化到0-360范围
        val normalizedY = ((yRotation % 360 + 360) % 360)

        // 转换为 ModelRotation 枚举
        val modelRotation = when (normalizedY) {
            0 -> ModelRotation.X0_Y0
            90 -> ModelRotation.X0_Y90
            180 -> ModelRotation.X0_Y180
            270 -> ModelRotation.X0_Y270
            else -> {
                // 非90度增量，使用最接近的值
                BedrockLoader.logger.warn(
                    "[BedrockModelLoadingPlugin] Transformation Y rotation $yRotation " +
                    "is not a multiple of 90°, using closest match"
                )
                when {
                    normalizedY < 45 -> ModelRotation.X0_Y0
                    normalizedY < 135 -> ModelRotation.X0_Y90
                    normalizedY < 225 -> ModelRotation.X0_Y180
                    normalizedY < 315 -> ModelRotation.X0_Y270
                    else -> ModelRotation.X0_Y0
                }
            }
        }

        return modelRotation
    }

    override fun onInitializeModelLoader(pluginContext: ModelLoadingPlugin.Context) {
        pluginContext.resolveModel().register { context ->
            val id = context.id()
            when {
                id.path.startsWith("block/") -> {
                    val identifier = Identifier.of(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistryClient.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = Identifier.of(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistryClient.itemModels[identifier]
                }
                else -> null
            }
        }

        BedrockAddonsRegistry.blocks.forEach { (id, v) ->
            val block = v as? BlockContext.BlockDataDriven ?: return@forEach
            val isBlockEntity = BedrockAddonsRegistry.blockEntities[id] != null

            pluginContext.registerBlockStateResolver(block) { context ->
                // 方块实体：注册占位符模型
                // 渲染类型是 INVISIBLE，所以这个模型不会被实际渲染
                // 但 Minecraft 仍然要求每个 blockstate 变体都有对应的模型
                if (isBlockEntity) {
                    block.stateManager.states.forEach { state ->
                        context.setModel(state, DelegatingUnbakedModel(Identifier.of("block/air")))
                    }
                    return@registerBlockStateResolver
                }

                // 普通方块：从注册表获取基础模型
                val baseUnbakedModel = BedrockAddonsRegistryClient.blockModels[id]
                if (baseUnbakedModel == null) {
                    BedrockLoaderClient.logger.warn("Block model not found for: $id, using missing model")
                    return@registerBlockStateResolver
                }

                // 获取基础geometry标识符（用于比较）
                val baseGeometryId = (baseUnbakedModel as? BedrockGeometryModel)?.let {
                    block.getGeometryIdentifier(block.defaultState)
                }

                // 为每个BlockState创建或复用模型
                block.stateManager.states.forEach { state ->
                    val stateGeometryId = block.getGeometryIdentifier(state)

                    // 检查是否需要加载不同的geometry
                    val unbakedModel = if (stateGeometryId != null &&
                                           stateGeometryId != baseGeometryId &&
                                           baseUnbakedModel is BedrockGeometryModel) {
                        // 需要切换geometry，创建新模型
                        val geometryFactory = BedrockAddonsRegistryClient.geometries[stateGeometryId]
                        if (geometryFactory != null) {
                            val stateMaterialInstances = block.getComponents(state).minecraftMaterialInstances
                            val materials = if (stateMaterialInstances != null) {
                                BedrockMaterialHelper.createMaterialsFromInstances(
                                    id.namespace,
                                    id,
                                    stateMaterialInstances
                                )
                            } else {
                                baseUnbakedModel.materials
                            }
                            geometryFactory.create(materials, id).also {
                                BedrockLoaderClient.logger.debug(
                                    "[BedrockModelLoadingPlugin] Created geometry variant for $id state $state: $stateGeometryId"
                                )
                            }
                        } else {
                            BedrockLoaderClient.logger.warn(
                                "[BedrockModelLoadingPlugin] Geometry '$stateGeometryId' not found for block $id, using base model"
                            )
                            baseUnbakedModel
                        }
                    } else {
                        // 使用基础geometry
                        baseUnbakedModel
                    }

                    // 应用transformation和材质变体
                    val finalModel = when (unbakedModel) {
                        // BedrockGeometryModel: 使用现有的变体系统（支持任意角度）
                        is BedrockGeometryModel -> unbakedModel.getModelVariant(block, state)

                        // JsonUnbakedModel: 应用 ModelRotation（仅支持90度增量）
                        is JsonUnbakedModel -> {
                            val components = block.getComponents(state)
                            val transformation = components?.minecraftTransformation

                            if (transformation != null) {
                                // 将transformation转换为ModelRotation并包装
                                val rotation = transformationToModelRotation(transformation)
                                RotatedJsonModel(unbakedModel, rotation)
                            } else {
                                unbakedModel
                            }
                        }

                        else -> unbakedModel
                    }
                    context.setModel(state, finalModel)
                }
            }
        }
    }
}