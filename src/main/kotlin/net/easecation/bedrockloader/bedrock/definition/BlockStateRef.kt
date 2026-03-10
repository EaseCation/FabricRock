package net.easecation.bedrockloader.bedrock.definition

import net.easecation.bedrockloader.util.identifierOf
import net.minecraft.util.Identifier

/**
 * Java 版 BlockState 字符串引用，格式：`namespace:id[prop1=val1,prop2=val2]`
 * 与 /setblock 命令及数据包规范一致，方括号内容为可选的固定 state 约束。
 */
data class BlockStateRef(
    val blockId: Identifier,
    val stateOverrides: Map<String, String>  // 属性名 -> 期望值（字符串）
) {
    companion object {
        /**
         * 解析形如 "namespace:id[prop=val,prop2=val2]" 的字符串。
         * 无 state 约束时，方括号可省略。
         */
        fun parse(str: String): BlockStateRef {
            val bracketIdx = str.indexOf('[')
            if (bracketIdx == -1) {
                return BlockStateRef(identifierOf(str), emptyMap())
            }
            val blockIdStr = str.substring(0, bracketIdx)
            val stateStr = str.substring(bracketIdx + 1, str.length - 1)  // 去掉 [ 和 ]
            val overrides = if (stateStr.isBlank()) emptyMap() else {
                stateStr.split(',').associate { entry ->
                    val eq = entry.indexOf('=')
                    if (eq == -1) entry.trim() to ""
                    else entry.substring(0, eq).trim() to entry.substring(eq + 1).trim()
                }
            }
            return BlockStateRef(identifierOf(blockIdStr), overrides)
        }
    }
}
