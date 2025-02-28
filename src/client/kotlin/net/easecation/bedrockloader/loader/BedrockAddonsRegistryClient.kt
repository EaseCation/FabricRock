package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.Identifier

object BedrockAddonsRegistryClient {
    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val blockEntityModels: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()
    val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val entityModel: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()
}