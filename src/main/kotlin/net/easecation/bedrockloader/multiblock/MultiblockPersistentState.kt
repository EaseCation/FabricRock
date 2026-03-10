package net.easecation.bedrockloader.multiblock

import net.easecation.bedrockloader.util.identifierOf
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtLong
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
//? if >=1.21.2 {
import net.minecraft.registry.RegistryWrapper
//?}
//? if >=1.21.11 {
import net.minecraft.world.PersistentStateType
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
//?}

/**
 * 多方块结构的世界持久化状态。
 * 存储 partPos → controllerPos 以及 controllerPos → (multiblockId, Set<partPos>) 的双向映射。
 *
 * 随存档持久化（ServerWorld 的 PersistentStateManager），保证服务器重启后
 * 破坏部件仍能正确触发整体拆解。
 */
class MultiblockPersistentState : PersistentState() {

    /** 部件位置 → 控制方块位置（BlockPos.asLong() 编码） */
    val partToController: MutableMap<Long, Long> = mutableMapOf()

    /** 控制方块位置 → 所属多方块的数据（identifier + 部件位置集合） */
    val controllerToData: MutableMap<Long, ControllerData> = mutableMapOf()

    data class ControllerData(
        /** 多方块 identifier，用于在控制方块已被移除时仍能查找定义 */
        val multiblockId: Identifier,
        /** 该多方块的所有部件位置（不含控制方块自身） */
        val parts: MutableSet<Long>
    ) {
        //? if >=1.21.11 {
        companion object {
            val CODEC: Codec<ControllerData> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Identifier.CODEC.fieldOf("multiblock_id").forGetter { it.multiblockId },
                    Codec.LONG.listOf().fieldOf("parts").forGetter { it.parts.toList() }
                ).apply(instance) { id, parts -> ControllerData(id, parts.toMutableSet()) }
            }
        }
        //?}
    }

    companion object {
        const val STATE_KEY = "bedrock_multiblock"

        //? if >=1.21.11 {
        private val PART_TO_CTRL_CODEC: Codec<MutableMap<Long, Long>> =
            Codec.unboundedMap(Codec.STRING.xmap(String::toLong, Long::toString), Codec.LONG)
                .xmap({ it.toMutableMap() }, { it })

        private val CTRL_TO_DATA_CODEC: Codec<MutableMap<Long, ControllerData>> =
            Codec.unboundedMap(Codec.STRING.xmap(String::toLong, Long::toString), ControllerData.CODEC)
                .xmap({ it.toMutableMap() }, { it })

        val CODEC: Codec<MultiblockPersistentState> = RecordCodecBuilder.create { instance ->
            instance.group(
                PART_TO_CTRL_CODEC.fieldOf("part_to_controller").forGetter { it.partToController },
                CTRL_TO_DATA_CODEC.fieldOf("controller_to_data").forGetter { it.controllerToData }
            ).apply(instance) { partToCtrl, ctrlToData ->
                MultiblockPersistentState().also { state ->
                    state.partToController.putAll(partToCtrl)
                    state.controllerToData.putAll(ctrlToData)
                }
            }
        }

        val STATE_TYPE: PersistentStateType<MultiblockPersistentState> =
            PersistentStateType(STATE_KEY, { MultiblockPersistentState() }, CODEC, null)

        fun getOrCreate(world: ServerWorld): MultiblockPersistentState {
            @Suppress("UNCHECKED_CAST")
            return world.persistentStateManager.getOrCreate(STATE_TYPE) as MultiblockPersistentState
        }
        //?} elif >=1.21.2 {
        /*
        private fun fromNbt(nbt: NbtCompound): MultiblockPersistentState {
            val state = MultiblockPersistentState()
            val list = nbt.getList("assemblies", net.minecraft.nbt.NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                val ctrlLong = entry.getLong("controller")
                val multiblockId = identifierOf(entry.getString("multiblock_id"))
                val parts = mutableSetOf<Long>()
                val partsNbt = entry.getList("parts", net.minecraft.nbt.NbtElement.LONG_TYPE.toInt())
                for (j in 0 until partsNbt.size) {
                    val partLong = partsNbt.getLong(j)
                    parts.add(partLong)
                    state.partToController[partLong] = ctrlLong
                }
                state.controllerToData[ctrlLong] = ControllerData(multiblockId, parts)
            }
            return state
        }

        fun getOrCreate(world: ServerWorld): MultiblockPersistentState {
            return world.persistentStateManager.getOrCreate(
                Type(
                    { MultiblockPersistentState() },
                    { nbt, _ -> fromNbt(nbt) },
                    null
                ),
                STATE_KEY
            )
        }
        *///?} else {

        /*private fun fromNbt(nbt: NbtCompound): MultiblockPersistentState {
            val state = MultiblockPersistentState()
            val list = nbt.getList("assemblies", net.minecraft.nbt.NbtElement.COMPOUND_TYPE.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                val ctrlLong = entry.getLong("controller")
                val multiblockId = identifierOf(entry.getString("multiblock_id"))
                val parts = mutableSetOf<Long>()
                val partsNbt = entry.getList("parts", net.minecraft.nbt.NbtElement.LONG_TYPE.toInt())
                for (j in 0 until partsNbt.size) {
                    val partLong = (partsNbt[j] as NbtLong).longValue()
                    parts.add(partLong)
                    state.partToController[partLong] = ctrlLong
                }
                state.controllerToData[ctrlLong] = ControllerData(multiblockId, parts)
            }
            return state
        }

        fun getOrCreate(world: ServerWorld): MultiblockPersistentState {
            return world.persistentStateManager.getOrCreate(
                PersistentState.Type(
                    { MultiblockPersistentState() },
                    { nbt -> fromNbt(nbt) },
                    null
                ),
                STATE_KEY
            )
        }
        *///?}
    }

    //? if <1.21.2 {
    
    /*override fun writeNbt(nbt: NbtCompound): NbtCompound {
        val list = NbtList()
        controllerToData.forEach { (ctrlLong, data) ->
            val entry = NbtCompound()
            entry.putLong("controller", ctrlLong)
            entry.putString("multiblock_id", data.multiblockId.toString())
            val partsNbt = NbtList()
            data.parts.forEach { partLong -> partsNbt.add(NbtLong.of(partLong)) }
            entry.put("parts", partsNbt)
            list.add(entry)
        }
        nbt.put("assemblies", list)
        return nbt
    }
    *///?} elif >=1.21.2 && <1.21.11 {
    /*
    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup): NbtCompound {
        val list = NbtList()
        controllerToData.forEach { (ctrlLong, data) ->
            val entry = NbtCompound()
            entry.putLong("controller", ctrlLong)
            entry.putString("multiblock_id", data.multiblockId.toString())
            val partsNbt = NbtList()
            data.parts.forEach { partLong -> partsNbt.add(NbtLong.of(partLong)) }
            entry.put("parts", partsNbt)
            list.add(entry)
        }
        nbt.put("assemblies", list)
        return nbt
    }
    *///?}

    /** 注册一次成功的组装，建立双向映射。 */
    fun registerAssembly(controllerPos: BlockPos, multiblockId: Identifier, partPositions: List<BlockPos>) {
        val ctrlLong = controllerPos.asLong()
        val partsLongs = partPositions.map { it.asLong() }
        controllerToData[ctrlLong] = ControllerData(multiblockId, partsLongs.toMutableSet())
        partsLongs.forEach { partLong ->
            partToController[partLong] = ctrlLong
        }
        markDirty()
    }

    /**
     * 注销一次组装，清除双向映射。
     * @return 被注销的所有部件位置列表（不含控制方块）
     */
    fun unregisterAssembly(controllerPos: BlockPos): List<BlockPos> {
        val ctrlLong = controllerPos.asLong()
        val data = controllerToData.remove(ctrlLong) ?: return emptyList()
        data.parts.forEach { partLong ->
            partToController.remove(partLong)
        }
        markDirty()
        return data.parts.map { BlockPos.fromLong(it) }
    }

    /** 获取部件位置对应的控制方块位置，若该位置不是已注册的部件则返回 null。 */
    fun getControllerPos(partPos: BlockPos): BlockPos? {
        return partToController[partPos.asLong()]?.let { BlockPos.fromLong(it) }
    }

    /** 获取控制方块位置对应的多方块 identifier，若该位置不是已注册的控制方块则返回 null。 */
    fun getMultiblockId(controllerPos: BlockPos): Identifier? {
        return controllerToData[controllerPos.asLong()]?.multiblockId
    }

    /** 判断某位置是否为已注册的控制方块。 */
    fun isController(pos: BlockPos): Boolean {
        return controllerToData.containsKey(pos.asLong())
    }

    /** 判断某位置是否为已注册的部件（不包括控制方块）。 */
    fun isPart(pos: BlockPos): Boolean {
        return partToController.containsKey(pos.asLong())
    }

    /** 判断某位置是否属于某个多方块（无论是控制方块还是部件）。 */
    fun isPartOfMultiblock(pos: BlockPos): Boolean {
        return isController(pos) || isPart(pos)
    }
}
