package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.block.Block
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.entity.EntityType
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object BedrockAddonsRegistry {

    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    val entityModel: MutableMap<Identifier, EntityModel<EntityDataDriven>> = mutableMapOf()
    val items: MutableMap<Identifier, Item> = mutableMapOf()
    val blocks: MutableMap<Identifier, Block> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityType<EntityDataDriven>> = mutableMapOf()
    val entityComponents: MutableMap<Identifier, EntityComponents> = mutableMapOf()

    fun getOrRegisterEntityType(id: Identifier): EntityType<EntityDataDriven> =
            entities.getOrElse(id) {
                Registry.register(Registries.ENTITY_TYPE, id, EntityDataDriven.buildEntityType(id)).also { entities[id] = it }
            }

}