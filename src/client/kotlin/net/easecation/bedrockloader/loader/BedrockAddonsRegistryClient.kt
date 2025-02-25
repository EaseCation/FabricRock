package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.Identifier

object BedrockAddonsRegistryClient {
    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val entityModel: MutableMap<Identifier, EntityModel<EntityDataDriven>> = mutableMapOf()
}