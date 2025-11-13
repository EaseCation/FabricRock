package net.easecation.bedrockloader.java.definition

import com.google.gson.annotations.SerializedName

/**
 * Java版方块Tag的JSON数据结构
 *
 * 对应Minecraft Java版数据包中的Tag文件格式：
 * `data/<namespace>/tags/blocks/<tag_name>.json`
 *
 * 示例：
 * ```json
 * {
 *   "replace": false,
 *   "values": [
 *     "minecraft:oak_log",
 *     "minecraft:birch_log",
 *     "#minecraft:logs"
 *   ]
 * }
 * ```
 *
 * @property replace 是否替换现有Tag（false表示追加，true表示完全覆盖）
 * @property values 方块ID列表或Tag引用列表（以#开头表示引用其他Tag）
 */
data class JavaBlockTag(
    val replace: Boolean = false,
    val values: List<String>
)
