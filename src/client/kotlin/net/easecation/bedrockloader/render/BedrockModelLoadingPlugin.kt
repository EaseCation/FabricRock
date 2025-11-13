package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoaderClient
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.util.Identifier

object BedrockModelLoadingPlugin : ModelLoadingPlugin {
    override fun onInitializeModelLoader(pluginContext: ModelLoadingPlugin.Context) {
        pluginContext.resolveModel().register { context ->
            val id = context.id()
            when {
                id.path.startsWith("block/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistryClient.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = Identifier(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistryClient.itemModels[identifier]
                }
                else -> null
            }
        }

        BedrockAddonsRegistry.blocks.forEach { (id, v) ->
            val block = v as? BlockContext.BlockDataDriven ?: return@forEach
            pluginContext.registerBlockStateResolver(block) { context ->
                // 直接从注册表获取基础模型
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
                    val finalModel = (unbakedModel as? BedrockGeometryModel)?.getModelVariant(block, state) ?: unbakedModel
                    context.setModel(state, finalModel)
                }
            }
        }
    }
}