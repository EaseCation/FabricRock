package net.easecation.bedrockloader.multiblock

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.minecraft.util.Identifier

/**
 * 从已加载的行为包上下文中读取多方块定义，注册到 MultiblockRegistry，
 * 并将"仅作为部件、从不作为控制方块"的方块 ID 加入 BedrockAddonsRegistry.multiblockPartOnlyIds。
 */
class MultiblockLoader(private val context: BedrockPackContext) {

    fun load() {
        val allControllerBlockIds = mutableSetOf<Identifier>()
        val allPartBlockIds = mutableSetOf<Identifier>()

        context.packs.forEach { pack ->
            pack.behavior.multiblocks.values.forEach { def ->
                BedrockLoader.logger.info("加载多方块定义: ${def.identifier} (${def.parts.size} 个部件)")
                MultiblockRegistry.register(def)

                def.parts.forEach { part ->
                    if (part.isController) {
                        allControllerBlockIds.add(part.blockId)
                    } else {
                        allPartBlockIds.add(part.blockId)
                    }
                }
            }
        }

        // 将仅作为部件（从未作为控制方块）的方块 ID 加入过滤集合
        // 这样它们在创造模式物品栏中会被隐藏（但仍可通过 /give 获取）
        val partOnlyIds = allPartBlockIds - allControllerBlockIds
        BedrockAddonsRegistry.multiblockPartOnlyIds.addAll(partOnlyIds)

        if (partOnlyIds.isNotEmpty()) {
            BedrockLoader.logger.info("标记 ${partOnlyIds.size} 个纯部件方块（隐藏在创造物品栏）: $partOnlyIds")
        }

        BedrockLoader.logger.info(
            "多方块定义加载完成：共 ${MultiblockRegistry.byIdentifier.size} 个结构，" +
            "涉及 ${MultiblockRegistry.byBlockId.size} 种方块"
        )
    }
}
