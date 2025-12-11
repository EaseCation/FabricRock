package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.annotations.SerializedName

/**
 * 基岩版方块面剔除规则定义
 *
 * 用于定义自定义方块模型中哪些面在相邻方向有完整方块时应该被剔除。
 *
 * 文件格式示例 (block_culling/xxx.json):
 * ```json
 * {
 *   "format_version": "1.20.60",
 *   "minecraft:block_culling_rules": {
 *     "description": {
 *       "identifier": "mymod:my_block_cull"
 *     },
 *     "rules": [
 *       {
 *         "geometry_part": {
 *           "bone": "bb_main",
 *           "cube": 0,
 *           "face": "north"
 *         },
 *         "direction": "north"
 *       }
 *     ]
 *   }
 * }
 * ```
 */
data class BlockCullingDefinition(
    val format_version: String,
    @SerializedName("minecraft:block_culling_rules")
    val blockCullingRules: BlockCullingRules
) {
    /**
     * 方块剔除规则容器
     */
    data class BlockCullingRules(
        val description: Description,
        val rules: List<CullingRule>
    )

    /**
     * 规则描述，包含唯一标识符
     */
    data class Description(
        val identifier: String
    )

    /**
     * 单条剔除规则
     *
     * @param geometry_part 指定要剔除的几何体部分（骨骼、立方体索引、面）
     * @param direction 当该方向有完整方块时，剔除指定的面
     */
    data class CullingRule(
        val geometry_part: GeometryPart,
        val direction: String  // "north", "south", "east", "west", "up", "down"
    )

    /**
     * 几何体部分标识
     *
     * @param bone 骨骼名称
     * @param cube 该骨骼中的立方体索引（从 0 开始）
     * @param face 该立方体的面方向（north/south/east/west/up/down）
     */
    data class GeometryPart(
        val bone: String,
        val cube: Int,
        val face: String  // "north", "south", "east", "west", "up", "down"
    )
}
