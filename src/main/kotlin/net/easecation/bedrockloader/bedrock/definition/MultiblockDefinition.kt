package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.annotations.SerializedName
import net.minecraft.util.Identifier

/**
 * 多方块拼合结构定义，对应行为包 multiblocks/<name>.json 文件。
 *
 * 支持两种模式：
 * - 单 Block ID 模式：所有部件使用同一方块 ID，通过自定义 blockstate 区分外观
 *   例: "block": "mymod:windmill[mymod:part=hub]"
 * - 多 Block ID 模式：每个部件是独立注册的方块
 *   例: "block": "mymod:windmill_hub"
 *
 * "block" 字段使用 Java 版原生 BlockState 字符串格式（与 /setblock 命令一致）。
 */
data class MultiblockDefinition(
    @SerializedName("format_version") val formatVersion: String,
    @SerializedName("bedrockloader:multiblock") val multiblock: Multiblock
) {

    data class Multiblock(
        /** 多方块结构唯一标识符 */
        val identifier: Identifier,
        /** 所有部件定义（包含控制方块） */
        val parts: List<Part>,
        /** 旋转配置，null 表示不支持旋转 */
        val rotation: Rotation?
    )

    data class Part(
        /** 相对于控制方块的偏移，格式 [x, y, z]，基准方向 North */
        val offset: List<Int>,
        /**
         * 要放置的方块，Java 版 BlockState 字符串格式：
         * "namespace:id[prop=val,...]" 或 "namespace:id"（无 state 约束）
         */
        val block: String,
        /** 是否为控制方块（玩家持有/放置的方块） */
        @SerializedName("is_controller") val isController: Boolean = false,
        /**
         * 是否随控制方块朝向旋转该部件的 facing_property 属性。
         * true：部件朝向 = 控制方块朝向旋转后的方向
         * false（默认）：部件朝向 = 与控制方块朝向相同（不额外旋转）
         */
        @SerializedName("rotate_facing") val rotateFacing: Boolean = false
    ) {
        /** 解析 block 字符串为 BlockStateRef（GSON 用 Unsafe 绕过构造函数，不可使用 lazy delegate） */
        val blockStateRef: BlockStateRef get() = BlockStateRef.parse(block)

        /** 方块 ID（去掉 state 部分） */
        val blockId: net.minecraft.util.Identifier get() = blockStateRef.blockId

        /** 固定的 state 约束（放置时强制设置），不参与旋转 */
        val stateOverrides: Map<String, String> get() = blockStateRef.stateOverrides
    }

    data class Rotation(
        /** 是否启用旋转（控制方块需有对应的方向 trait） */
        val enabled: Boolean,
        /**
         * 用于旋转的方向属性名，如 "minecraft:cardinal_direction"。
         * 该属性的值会在放置时根据玩家朝向自动设置。
         */
        @SerializedName("facing_property") val facingProperty: String?
    )
}
