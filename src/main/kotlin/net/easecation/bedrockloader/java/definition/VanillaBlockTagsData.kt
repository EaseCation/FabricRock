package net.easecation.bedrockloader.java.definition

/**
 * 基岩版原生方块Tag数据结构
 *
 * 对应 `vanilla_block_tags.json` 文件格式，包含基岩版官方定义的方块Tag及其对应的方块列表。
 *
 * 文件格式示例：
 * ```json
 * {
 *   "columns": {
 *     "tag": { "name": "Tag", "sortable": true },
 *     "vanilla_usage": { "name": "Vanilla Usage" },
 *     "functionality": { "name": "Functionality" }
 *   },
 *   "rows": [
 *     {
 *       "tag": "`dirt`",
 *       "vanilla_usage": ["`minecraft:dirt`", "`minecraft:grass_block`"],
 *       "functionality": "Used for grass spread and farmland"
 *     }
 *   ]
 * }
 * ```
 *
 * @property columns 列定义（包含metadata，实际解析时不使用）
 * @property rows Tag数据行列表
 */
data class VanillaBlockTagsData(
    val columns: Map<String, Any>,
    val rows: List<TagRow>
)

/**
 * 单个Tag的数据行
 *
 * @property tag Tag名称（被反引号包裹，如 `` `dirt` `` 或 `` `minecraft:crop` ``）
 * @property vanilla_usage 使用该Tag的方块ID列表（每个ID被反引号包裹）
 * @property functionality Tag的功能描述（可选，仅用于文档说明）
 */
data class TagRow(
    val tag: String,
    val vanilla_usage: List<String>,
    val functionality: String? = null
)
