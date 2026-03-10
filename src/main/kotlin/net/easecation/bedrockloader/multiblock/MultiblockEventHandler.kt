package net.easecation.bedrockloader.multiblock

import net.easecation.bedrockloader.BedrockLoader
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.registry.Registries
import net.minecraft.server.world.ServerWorld

/**
 * 注册多方块相关的 Fabric 事件监听器。
 * 统一处理控制方块和部件方块被玩家破坏时的整体拆解逻辑。
 */
object MultiblockEventHandler {

    fun register() {
        // 监听玩家破坏方块事件（在实际破坏前触发）
        PlayerBlockBreakEvents.BEFORE.register { world, player, pos, state, _ ->
            val blockId = Registries.BLOCK.getId(state.block)

            // 快速过滤：若该方块 ID 不参与任何多方块定义，直接放行
            if (!MultiblockRegistry.byBlockId.containsKey(blockId)) return@register true

            val serverWorld = world as? ServerWorld ?: return@register true
            val persistentState = MultiblockPersistentState.getOrCreate(serverWorld)

            // 判断该位置是否属于已组装的多方块结构
            if (!persistentState.isPartOfMultiblock(pos)) return@register true

            // 找到控制方块位置
            val controllerPos = when {
                persistentState.isController(pos) -> pos
                else -> persistentState.getControllerPos(pos)
            } ?: return@register true

            // 通过持久化状态中存储的 multiblockId 找到定义（即使控制方块已不存在也能查找）
            val multiblockId = persistentState.getMultiblockId(controllerPos) ?: return@register true
            val def = MultiblockRegistry.byIdentifier[multiblockId] ?: run {
                BedrockLoader.logger.warn("找不到多方块定义：$multiblockId，直接清理持久化数据")
                persistentState.unregisterAssembly(controllerPos)
                return@register true
            }

            // 触发整体拆解（disassemble 会移除包括当前位置在内的所有方块）
            // 创意模式不掉落物品
            BedrockLoader.logger.debug("玩家破坏多方块 $multiblockId 的方块 @$pos，触发整体拆解")
            MultiblockAssembler.disassemble(serverWorld, controllerPos, def, persistentState, dropItems = !player.isCreative)

            // 返回 false 取消原始的单方块破坏事件（我们已在 disassemble 中统一处理）
            false
        }
    }
}
