package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.Identifier

object BedrockAddonsRegistryClient {
    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val blockEntityModels: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()
    val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val entityModel: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()

    /**
     * 存储每个namespace对应的资源包上下文，用于动态创建材质映射
     */
    val packContexts: MutableMap<String, BedrockPackContext> = mutableMapOf()
}