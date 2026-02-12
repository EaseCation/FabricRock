package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.EntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.util.Identifier

object BedrockAddonsRegistry {
    val items: MutableMap<Identifier, Item> = mutableMapOf()
    val blocks: MutableMap<Identifier, Block> = mutableMapOf()
    val blockEntities: MutableMap<Identifier, BlockEntityType<BlockEntityDataDriven>> = mutableMapOf()
    val entities: MutableMap<Identifier, EntityType<EntityDataDriven>> = mutableMapOf()
    val entityComponents: MutableMap<Identifier, EntityComponents> = mutableMapOf()

    // 物品到包的映射（用于按包分组创造模式选项卡）
    val itemToPackMapping: MutableMap<Identifier, String> = mutableMapOf()

    // 方块上下文映射（用于导出方块状态映射）
    val blockContexts: MutableMap<Identifier, BlockContext> = mutableMapOf()

    /**
     * 按包ID获取物品列表
     * @param packId 包UUID
     * @return 该包注册的所有物品
     */
    fun getItemsByPack(packId: String): List<Item> {
        return itemToPackMapping
            .filter { it.value == packId }
            .mapNotNull { items[it.key] }
    }

    /**
     * 按包ID获取方块物品列表（用于智能图标选择）
     * @param packId 包UUID
     * @return 该包注册的所有方块物品
     */
    fun getBlockItemsByPack(packId: String): List<Item> {
        return itemToPackMapping
            .filter { it.value == packId }
            .mapNotNull {
                val item = items[it.key]
                if (item is BlockItem) item else null
            }
    }
}