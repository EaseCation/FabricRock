package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.BedrockLoaderClient
import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockAddonsRegistryClient
//? if <1.21.4 {
import net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel
//?}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
//? if >=1.21.5 {
/*import net.minecraft.block.BlockState
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.BlockStateModel
import net.minecraft.client.render.model.ResolvableModel
import net.minecraft.client.render.model.SimpleBlockStateModel
import net.minecraft.client.render.model.SimpleModel
import net.minecraft.client.render.model.json.ModelVariant
import net.minecraft.util.math.AxisRotation
*///?} elif >=1.21.4 {
/*import net.minecraft.block.BlockState
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.Baker
import net.minecraft.client.render.model.GroupableModel
import net.minecraft.client.render.model.ResolvableModel
*///?}
import net.minecraft.client.render.model.ModelRotation
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.easecation.bedrockloader.util.identifierOf
import net.minecraft.util.Identifier

object BedrockModelLoadingPlugin : ModelLoadingPlugin {
    /**
     * 从 ComponentTransformation 提取归一化的 Y 轴旋转角度
     * 仅支持 90 度增量（0°/90°/180°/270°），忽略缩放和平移
     */
    private fun getYRotationDegrees(transformation: ComponentTransformation): Int {
        val rotation = transformation.rotation
        val yRotation = if (rotation != null && rotation.size >= 2) {
            rotation[1].toInt()
        } else {
            0
        }
        val normalizedY = ((yRotation % 360 + 360) % 360)
        return when (normalizedY) {
            0, 90, 180, 270 -> normalizedY
            else -> {
                BedrockLoader.logger.warn(
                    "[BedrockModelLoadingPlugin] Transformation Y rotation $yRotation " +
                    "is not a multiple of 90°, using closest match"
                )
                when {
                    normalizedY < 45 -> 0
                    normalizedY < 135 -> 90
                    normalizedY < 225 -> 180
                    normalizedY < 315 -> 270
                    else -> 0
                }
            }
        }
    }

    //? if <1.21.11 {
    private fun transformationToModelRotation(transformation: ComponentTransformation): ModelRotation {
        return when (getYRotationDegrees(transformation)) {
            90 -> ModelRotation.X0_Y90
            180 -> ModelRotation.X0_Y180
            270 -> ModelRotation.X0_Y270
            else -> ModelRotation.X0_Y0
        }
    }
    //?}

    //? if >=1.21.2 {
    /*override fun initialize(pluginContext: ModelLoadingPlugin.Context) {
    *///?} else {
    override fun onInitializeModelLoader(pluginContext: ModelLoadingPlugin.Context) {
    //?}
        //? if <1.21.4 {
        pluginContext.resolveModel().register { context ->
            val id = context.id()
            when {
                id.path.startsWith("block/") -> {
                    val identifier = identifierOf(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistryClient.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = identifierOf(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistryClient.itemModels[identifier]
                }
                else -> null
            }
        }
        //?} elif <1.21.5 {
        /*pluginContext.modifyModelOnLoad().register { model, context ->
            val id = context.id()
            val customModel = when {
                id.path.startsWith("block/") -> {
                    val identifier = identifierOf(id.namespace, id.path.substring("block/".length))
                    BedrockAddonsRegistryClient.blockModels[identifier]
                }
                id.path.startsWith("item/") -> {
                    val identifier = identifierOf(id.namespace, id.path.substring("item/".length))
                    BedrockAddonsRegistryClient.itemModels[identifier]
                }
                else -> null
            }
            when {
                customModel is net.minecraft.client.render.model.UnbakedModel -> customModel
                customModel is BedrockGeometryModel -> {
                    object : net.minecraft.client.render.model.UnbakedModel {
                        override fun resolve(resolver: ResolvableModel.Resolver) {}
                        override fun bake(
                            textures: net.minecraft.client.render.model.ModelTextures,
                            baker: Baker,
                            settings: net.minecraft.client.render.model.ModelBakeSettings,
                            ambientOcclusion: Boolean,
                            isSideLit: Boolean,
                            transformation: net.minecraft.client.render.model.json.ModelTransformation
                        ): BakedModel? {
                            return customModel.bake(baker)
                        }
                    }
                }
                else -> model
            }
        }
        *///?}

        //? if >=1.21.5 {
        /*// 1.21.5+: 标准立方体和平面图标物品通过物理 models/item/^.json 文件加载（原版管线处理）
        // 仅 BedrockGeometryModel 类型需要通过 modifyItemModelBeforeBake 拦截
        pluginContext.modifyItemModelBeforeBake().register { model, context ->
            val itemId = context.itemId()
            val customModel = BedrockAddonsRegistryClient.itemModels[itemId]
            if (customModel is BedrockGeometryModel) {
                BedrockItemModelUnbaked(customModel)
            } else {
                model
            }
        }
        *///?}

        BedrockAddonsRegistry.blocks.forEach { (id, v) ->
            val block = v as? BlockContext.BlockDataDriven ?: return@forEach
            val isBlockEntity = BedrockAddonsRegistry.blockEntities[id] != null

            pluginContext.registerBlockStateResolver(block) { context ->
                // 方块实体：注册占位符模型
                // 渲染类型是 INVISIBLE，所以这个模型不会被实际渲染
                // 但 Minecraft 仍然要求每个 blockstate 变体都有对应的模型
                if (isBlockEntity) {
                    block.stateManager.states.forEach { state ->
                        //? if <1.21.4 {
                        context.setModel(state, net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel(identifierOf("block/air")))
                        //?} elif <1.21.5 {
                        /*context.setModel(state, object : GroupableModel {
                            override fun resolve(resolver: ResolvableModel.Resolver) {
                                resolver.resolve(identifierOf("block/air"))
                            }
                            override fun bake(baker: Baker): BakedModel? = baker.bake(identifierOf("block/air"), ModelRotation.X0_Y0)
                            override fun getEqualityGroup(state: BlockState): Any = "air"
                        })
                        *///?} else {
                        /*context.setModel(state, SimpleBlockStateModel.Unbaked(ModelVariant(identifierOf("block/air"))).cached())
                        *///?}
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

                    //? if >=1.21.5 {
                    /*val unbakedGrouped: BlockStateModel.UnbakedGrouped = when (unbakedModel) {
                        is BedrockGeometryModel -> unbakedModel.getModelVariant(block, state)
                        else -> {
                            val modelId = identifierOf(id.namespace, "block/${id.path}")
                            val components = block.getComponents(state)
                            val transformation = components?.minecraftTransformation
                            var variant = ModelVariant(modelId)
                            if (transformation != null) {
                                val yDeg = getYRotationDegrees(transformation)
                                variant = when (yDeg) {
                                    90 -> variant.withRotationY(AxisRotation.R90)
                                    180 -> variant.withRotationY(AxisRotation.R180)
                                    270 -> variant.withRotationY(AxisRotation.R270)
                                    else -> variant
                                }
                            }
                            SimpleBlockStateModel.Unbaked(variant).cached()
                        }
                    }
                    context.setModel(state, unbakedGrouped)
                    *///?} elif >=1.21.4 {
                    /*val finalModel = when (unbakedModel) {
                        is BedrockGeometryModel -> unbakedModel.getModelVariant(block, state)
                        is JsonUnbakedModel -> {
                            val components = block.getComponents(state)
                            val transformation = components?.minecraftTransformation
                            if (transformation != null) {
                                val rotation = transformationToModelRotation(transformation)
                                RotatedJsonModel(unbakedModel, rotation)
                            } else {
                                unbakedModel
                            }
                        }
                        else -> unbakedModel
                    }
                    val groupableModel: GroupableModel = when (finalModel) {
                        is BedrockGeometryModel -> finalModel
                        else -> {
                            val modelId = identifierOf(id.namespace, "block/${id.path}")
                            val rotation = if (finalModel is RotatedJsonModel) {
                                finalModel.rotation
                            } else ModelRotation.X0_Y0
                            object : GroupableModel {
                                override fun resolve(resolver: ResolvableModel.Resolver) {
                                    resolver.resolve(modelId)
                                }
                                override fun bake(baker: Baker): BakedModel? = baker.bake(modelId, rotation)
                                override fun getEqualityGroup(state: BlockState): Any = Pair(modelId, rotation)
                            }
                        }
                    }
                    context.setModel(state, groupableModel)
                    *///?} else {
                    val finalModel = when (unbakedModel) {
                        is BedrockGeometryModel -> unbakedModel.getModelVariant(block, state)
                        is JsonUnbakedModel -> {
                            val components = block.getComponents(state)
                            val transformation = components?.minecraftTransformation
                            if (transformation != null) {
                                val rotation = transformationToModelRotation(transformation)
                                RotatedJsonModel(unbakedModel, rotation)
                            } else {
                                unbakedModel
                            }
                        }
                        else -> unbakedModel
                    }
                    context.setModel(state, finalModel)
                    //?}
                }
            }
        }
    }
}