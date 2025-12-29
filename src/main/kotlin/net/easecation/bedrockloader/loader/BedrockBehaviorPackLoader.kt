package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.entity.BlockEntityDataDriven
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.loader.error.LoadingError
import net.easecation.bedrockloader.loader.error.LoadingErrorCollector
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.SpawnEggItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class BedrockBehaviorPackLoader(
        val context: BedrockPackContext
) {

    @OptIn(ExperimentalStdlibApi::class)
    fun load() {
        // 按包注册（保持加载顺序）
        context.packs.forEach { packContext ->
            loadPackContent(packContext)
        }
    }

    /**
     * 加载单个包的内容
     * @param packContext 包上下文
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun loadPackContent(packContext: net.easecation.bedrockloader.loader.context.SinglePackContext) {
        val packId = packContext.packId
        val packName = packContext.packInfo.name

        BedrockLoader.logger.info("加载行为包内容: $packName [$packId]")

        // 注册方块
        packContext.behavior.blocks.forEach { (id, beh) ->
            try {
                BedrockLoader.logger.info("Registering block $id from pack $packName")
                val block = BlockContext.create(id, beh)
                Registry.register(Registries.BLOCK, id, block)
                BedrockAddonsRegistry.blocks[id] = block

                val item = BlockItem(block, Item.Settings())
                Registry.register(Registries.ITEM, id, item)
                BedrockAddonsRegistry.items[id] = item

                // 关键：记录物品到包的映射
                BedrockAddonsRegistry.itemToPackMapping[id] = packId

                // 方块实体
                beh.components.neteaseBlockEntity?.let { blockEntity ->
                    BedrockLoader.logger.info("Registering block entity $id")
                    val blockEntityType = BlockEntityDataDriven.buildBlockEntityType(id)
                    Registry.register(Registries.BLOCK_ENTITY_TYPE, id, blockEntityType)
                    BedrockAddonsRegistry.blockEntities[id] = blockEntityType
                }
            } catch (e: Exception) {
                LoadingErrorCollector.addError(
                    source = "$id (包: $packName)",
                    phase = LoadingError.Phase.BLOCK_REGISTER,
                    message = "加载方块失败: ${e.message}",
                    exception = e
                )
            }
        }

        // 注册实体
        packContext.behavior.entities.forEach { (id, beh) ->
            try {
                BedrockLoader.logger.info("Registering entity $id from pack $packName")
                // entity type
                val entityType = EntityDataDriven.buildEntityType(id)
                Registry.register(Registries.ENTITY_TYPE, id, entityType)
                BedrockAddonsRegistry.entities[id] = entityType
                BedrockAddonsRegistry.entityComponents[id] = beh.components

                // entity attributes
                FabricDefaultAttributeRegistry.register(entityType, EntityDataDriven.buildEntityAttributes(beh.components))

                // spawn egg
                if (beh.description.is_spawnable == true) {
                    val clientEntity = packContext.resource.entities[id]?.description
                    val entityName = id.path
                    val itemIdentifier = Identifier(id.namespace, "${entityName}_spawn_egg")
                    val spawnEggItem = SpawnEggItem(
                        entityType,
                        clientEntity?.spawn_egg?.base_color?.replace("#", "")?.hexToInt(HexFormat.Default) ?: 0xffffff,
                        clientEntity?.spawn_egg?.overlay_color?.replace("#", "")?.hexToInt(HexFormat.Default) ?: 0xffffff,
                        Item.Settings()
                    )
                    Registry.register(Registries.ITEM, itemIdentifier, spawnEggItem)
                    BedrockAddonsRegistry.items[itemIdentifier] = spawnEggItem

                    // 关键：记录刷怪蛋到包的映射
                    BedrockAddonsRegistry.itemToPackMapping[itemIdentifier] = packId
                }
            } catch (e: Exception) {
                LoadingErrorCollector.addError(
                    source = "$id (包: $packName)",
                    phase = LoadingError.Phase.ENTITY_REGISTER,
                    message = "加载实体失败: ${e.message}",
                    exception = e
                )
            }
        }
    }

}