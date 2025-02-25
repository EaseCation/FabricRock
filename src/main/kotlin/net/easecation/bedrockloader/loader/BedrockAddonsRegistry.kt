package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.minecraft.block.Block
import net.minecraft.entity.EntityType
import net.minecraft.item.Item
import net.minecraft.util.Identifier

object BedrockAddonsRegistry {
    val items: MutableMap<Identifier, Item> = mutableMapOf()
    val blocks: MutableMap<Identifier, Block> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityType<EntityDataDriven>> = mutableMapOf()
    val entityComponents: MutableMap<Identifier, EntityComponents> = mutableMapOf()
}