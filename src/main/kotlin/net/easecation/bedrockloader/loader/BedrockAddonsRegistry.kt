package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.minecraft.block.Block
import net.minecraft.entity.EntityType
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object BedrockAddonsRegistry {

    val items: MutableMap<Identifier, Item> = mutableMapOf()
    val blocks: MutableMap<Identifier, Block> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityType<EntityDataDriven>> = mutableMapOf()
    val entityComponents: MutableMap<Identifier, EntityComponents> = mutableMapOf()
    val models: MutableMap<String, BedrockGeometryModel> = mutableMapOf()

    fun getOrRegisterEntityType(id: Identifier): EntityType<EntityDataDriven> =
            entities.getOrElse(id) {
                Registry.register(Registry.ENTITY_TYPE, id, EntityDataDriven.buildEntityType(id)).also { entities[id] = it }
            }

}