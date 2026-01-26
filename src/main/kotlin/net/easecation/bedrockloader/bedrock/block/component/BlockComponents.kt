package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

data class BlockComponents(
    @SerializedName("minecraft:material_instances") val minecraftMaterialInstances: ComponentMaterialInstances?,
    @SerializedName("minecraft:collision_box") val minecraftCollisionBox: ComponentCollisionBox?,
    @SerializedName("minecraft:selection_box") val minecraftSelectionBox: ComponentSelectionBox?,
    @SerializedName("minecraft:display_name") val minecraftDisplayName: String?,
    @SerializedName("minecraft:geometry") val minecraftGeometry: ComponentGeometry?,
    @SerializedName("minecraft:placement_filter") val minecraftPlacementFilter: ComponentPlacementFilter?,
    @SerializedName("minecraft:light_emission") val minecraftLightEmission: Int?,
    @SerializedName("minecraft:light_dampening") val minecraftLightDampening: Int?,
    @SerializedName("minecraft:destructible_by_mining") val minecraftDestructibleByMining: ComponentDestructibleByMining?,
    @SerializedName("minecraft:transformation") val minecraftTransformation: ComponentTransformation?,
    @SerializedName("minecraft:map_color") val minecraftMapColor: ComponentMapColor?,
    @SerializedName("netease:block_entity") val neteaseBlockEntity: ComponentBlockEntity?,
    @SerializedName("netease:face_directional") val neteaseFaceDirectional: ComponentFaceDirectional?,

    /**
     * 方块标签集合，从 tag:* 组件中提取
     *
     * 例如 JSON 中的 "tag:wood": {}, "tag:minecraft:is_axe_item_destructible": {}
     * 会被提取为 ["wood", "minecraft:is_axe_item_destructible"]
     *
     * 注意：此字段由自定义反序列化器填充，不使用 @SerializedName
     */
    val tags: Set<String> = emptySet()
) {
    fun mergeComponents(components: BlockComponents): BlockComponents {
        return BlockComponents(
            minecraftMaterialInstances = components.minecraftMaterialInstances ?: minecraftMaterialInstances,
            minecraftCollisionBox = components.minecraftCollisionBox ?: minecraftCollisionBox,
            minecraftSelectionBox = components.minecraftSelectionBox ?: minecraftSelectionBox,
            minecraftDisplayName = components.minecraftDisplayName ?: minecraftDisplayName,
            minecraftGeometry = components.minecraftGeometry ?: minecraftGeometry,
            minecraftPlacementFilter = components.minecraftPlacementFilter ?: minecraftPlacementFilter,
            minecraftLightEmission = components.minecraftLightEmission ?: minecraftLightEmission,
            minecraftLightDampening = components.minecraftLightDampening ?: minecraftLightDampening,
            minecraftDestructibleByMining = components.minecraftDestructibleByMining ?: minecraftDestructibleByMining,
            minecraftTransformation = components.minecraftTransformation ?: minecraftTransformation,
            minecraftMapColor = components.minecraftMapColor ?: minecraftMapColor,
            neteaseBlockEntity = components.neteaseBlockEntity ?: neteaseBlockEntity,
            neteaseFaceDirectional = components.neteaseFaceDirectional ?: neteaseFaceDirectional,
            tags = this.tags + components.tags  // 合并 tags（集合并集）
        )
    }

    /**
     * TypeAdapterFactory 实现，用于提取 tag:* 组件
     *
     * 基岩版方块可以定义动态的 tag 组件，例如：
     * ```json
     * {
     *   "components": {
     *     "tag:wood": {},
     *     "tag:minecraft:is_axe_item_destructible": {},
     *     "minecraft:geometry": "geometry.cube"
     *   }
     * }
     * ```
     *
     * 此 Factory 会：
     * 1. 通过 getDelegateAdapter() 获取默认的 TypeAdapter（自动包含所有已注册的 sealed class 适配器）
     * 2. 提取所有 tag:* 前缀的键，去除前缀后存入 tags 集合
     * 3. 过滤掉 tag:* 字段，使用默认 TypeAdapter 反序列化标准字段
     * 4. 返回包含 tags 的 BlockComponents 对象
     *
     * 优势：
     * - 零维护成本：自动继承所有已注册的 TypeAdapter，无需手动维护列表
     * - 避免递归：getDelegateAdapter() 跳过当前 factory，使用下一个匹配的适配器
     * - 性能优秀：复用父 Gson 的所有适配器缓存
     * - 符合 Gson 最佳实践
     */
    class Factory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            // 仅处理 BlockComponents 类型
            if (type.rawType != BlockComponents::class.java) return null

            // 获取默认的 BlockComponents TypeAdapter（跳过当前 factory）
            // 此 adapter 会自动使用 Gson 中已注册的所有 TypeAdapter（包括 sealed class 的）
            @Suppress("UNCHECKED_CAST")
            val defaultAdapter = gson.getDelegateAdapter(this, type) as TypeAdapter<BlockComponents>

            return object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T?) {
                    defaultAdapter.write(out, value as BlockComponents?)
                }

                override fun read(reader: JsonReader): T {
                    // 读取为 JsonObject
                    val jsonObject = Streams.parse(reader).asJsonObject

                    // 步骤1：提取所有 tag:* 前缀的键
                    val extractedTags = mutableSetOf<String>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key.startsWith("tag:")) {
                            // 移除 "tag:" 前缀，存储实际的 tag 名称
                            extractedTags.add(entry.key.substring(4))
                        }
                    }

                    // 步骤2：创建临时 JsonObject，过滤掉 tag:* 字段
                    val standardFieldsObj = JsonObject()
                    for (entry in jsonObject.entrySet()) {
                        if (!entry.key.startsWith("tag:")) {
                            standardFieldsObj.add(entry.key, entry.value)
                        }
                    }

                    // 步骤3：使用默认 adapter 反序列化标准字段
                    // defaultAdapter 会自动处理所有 sealed class 和其他自定义类型
                    val standardComponents = defaultAdapter.fromJsonTree(standardFieldsObj)

                    // 步骤4：返回包含提取的 tags 的结果
                    @Suppress("UNCHECKED_CAST")
                    return standardComponents.copy(tags = extractedTags) as T
                }
            }
        }
    }
}
