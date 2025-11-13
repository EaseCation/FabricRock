package net.easecation.bedrockloader.block.condition

import net.easecation.bedrockloader.BedrockLoader

/**
 * Molang条件表达式编译器
 *
 * 使用模式匹配+函数映射策略，将基岩版Molang条件字符串预编译为
 * 可执行的CompiledCondition对象，避免运行时重复解析。
 *
 * 支持的基岩版原生语法：
 * - query.block_state('property') == 'value'
 * - query.block_state('property') != 'value'
 * - query.block_neighbor_has_all_tags(x, y, z, 'tag1', 'tag2', ...)
 * - query.block_neighbor_has_any_tag(x, y, z, 'tag1', 'tag2', ...)
 * - 逻辑运算：&&, ||, !
 * - 括号分组：(...)
 */
object MolangConditionCompiler {

    // ========== 正则表达式模式 ==========

    /**
     * 匹配 query.block_state('property') == 'value'
     *
     * 捕获组：
     * - key: 属性名称
     * - operator: 运算符（== 或 !=）
     * - value: 期望值
     */
    private val blockStateRegex = """^\s*q(?:uery)?\s*\.\s*block_state\s*\(\s*'(?<key>[^']+)'\s*\)\s*(?<operator>==|!=)\s*'(?<value>[^']+)'\s*$""".toRegex()

    /**
     * 匹配 query.block_neighbor_has_all_tags(x, y, z, 'tag1', 'tag2', ...)
     *       query.block_neighbor_has_any_tag(x, y, z, 'tag1', 'tag2', ...)
     *
     * 捕获组：
     * - mode: all 或 any
     * - x, y, z: 坐标偏移
     * - tags: 标签列表（需要后续提取）
     */
    private val neighborTagsRegex = """^\s*q(?:uery)?\s*\.\s*block_neighbor_has_(?<mode>all|any)_tags?\s*\(\s*(?<x>-?\d+)\s*,\s*(?<y>-?\d+)\s*,\s*(?<z>-?\d+)\s*(?<tags>(?:,\s*'[^']+')+)\s*\)\s*$""".toRegex()

    /**
     * 提取标签列表：从 ", 'tag1', 'tag2'" 提取 ["tag1", "tag2"]
     */
    private val tagExtractRegex = """'([^']+)'""".toRegex()

    /**
     * 逻辑运算符优先级（数字越大优先级越低）
     */
    private enum class Operator(val symbol: String, val precedence: Int) {
        NOT("!", 1),
        AND("&&", 2),
        OR("||", 3)
    }

    // ========== 公共API ==========

    /**
     * 编译Molang条件字符串
     *
     * @param condition 基岩版Molang条件表达式
     * @return 预编译的条件对象
     * @throws IllegalArgumentException 如果条件字符串无法解析
     */
    fun compile(condition: String): CompiledCondition {
        val trimmed = condition.trim()
        if (trimmed.isEmpty()) {
            BedrockLoader.logger.warn("[MolangCompiler] Empty condition string, returning AlwaysTrue")
            return CompiledCondition.AlwaysTrue
        }

        return try {
            parseExpression(trimmed)
        } catch (e: Exception) {
            BedrockLoader.logger.error("[MolangCompiler] Failed to compile condition: $condition", e)
            throw IllegalArgumentException("Failed to compile Molang condition: $condition", e)
        }
    }

    // ========== 解析逻辑 ==========

    /**
     * 解析表达式（处理逻辑运算符）
     */
    private fun parseExpression(expr: String): CompiledCondition {
        // 处理括号分组
        val withoutParens = removeOuterParentheses(expr)
        if (withoutParens != expr) {
            return parseExpression(withoutParens)
        }

        // 查找优先级最低的运算符（从右往左，优先处理OR）
        val orPos = findTopLevelOperator(expr, "||")
        if (orPos >= 0) {
            val left = expr.substring(0, orPos).trim()
            val right = expr.substring(orPos + 2).trim()
            return CompiledCondition.Or(
                parseExpression(left),
                parseExpression(right)
            )
        }

        // 处理AND
        val andPos = findTopLevelOperator(expr, "&&")
        if (andPos >= 0) {
            val left = expr.substring(0, andPos).trim()
            val right = expr.substring(andPos + 2).trim()
            return CompiledCondition.And(
                parseExpression(left),
                parseExpression(right)
            )
        }

        // 处理NOT
        if (expr.trim().startsWith("!")) {
            val inner = expr.trim().substring(1).trim()
            return CompiledCondition.Not(parseExpression(inner))
        }

        // 解析原子条件
        return parseAtomicCondition(expr)
    }

    /**
     * 解析原子条件（query.block_state 或 query.block_neighbor_has_*_tags）
     */
    private fun parseAtomicCondition(expr: String): CompiledCondition {
        // 尝试匹配 block_state
        blockStateRegex.matchEntire(expr)?.let { match ->
            val key = match.groups["key"]!!.value
            val operator = match.groups["operator"]!!.value
            val value = match.groups["value"]!!.value
            return CompiledCondition.BlockStateCheck(key, operator, value)
        }

        // 尝试匹配 block_neighbor_has_all_tags / block_neighbor_has_any_tag
        neighborTagsRegex.matchEntire(expr)?.let { match ->
            val mode = match.groups["mode"]!!.value
            val x = match.groups["x"]!!.value.toInt()
            val y = match.groups["y"]!!.value.toInt()
            val z = match.groups["z"]!!.value.toInt()
            val tagsStr = match.groups["tags"]!!.value

            // 提取标签列表
            val tags = tagExtractRegex.findAll(tagsStr).map { it.groupValues[1] }.toList()

            if (tags.isEmpty()) {
                BedrockLoader.logger.warn("[MolangCompiler] No tags found in neighbor query: $expr")
                return CompiledCondition.AlwaysFalse
            }

            return CompiledCondition.NeighborTagCheck(
                offsetX = x,
                offsetY = y,
                offsetZ = z,
                tags = tags,
                matchAll = mode == "all"
            )
        }

        // 无法识别的条件
        BedrockLoader.logger.warn("[MolangCompiler] Unrecognized condition pattern: $expr")
        throw IllegalArgumentException("Unsupported Molang condition: $expr")
    }

    /**
     * 移除最外层的括号（如果存在）
     *
     * 例如："(a && b)" -> "a && b"
     */
    private fun removeOuterParentheses(expr: String): String {
        val trimmed = expr.trim()
        if (!trimmed.startsWith("(") || !trimmed.endsWith(")")) {
            return trimmed
        }

        // 检查是否是匹配的最外层括号
        var depth = 0
        for (i in trimmed.indices) {
            when (trimmed[i]) {
                '(' -> depth++
                ')' -> depth--
            }
            // 如果在中间某处深度归零，说明不是最外层括号
            if (depth == 0 && i < trimmed.length - 1) {
                return trimmed
            }
        }

        // 确实是最外层括号，移除
        return trimmed.substring(1, trimmed.length - 1).trim()
    }

    /**
     * 查找顶层（非括号内）的运算符位置
     *
     * @param expr 表达式
     * @param operator 运算符字符串（"&&" 或 "||"）
     * @return 运算符的起始位置，未找到返回-1
     */
    private fun findTopLevelOperator(expr: String, operator: String): Int {
        var depth = 0
        var i = 0

        while (i <= expr.length - operator.length) {
            when (expr[i]) {
                '(' -> depth++
                ')' -> depth--
            }

            // 仅在顶层查找
            if (depth == 0 && expr.substring(i).startsWith(operator)) {
                return i
            }

            i++
        }

        return -1
    }

    // ========== 调试工具 ==========

    /**
     * 将编译后的条件转换为可读字符串（用于调试）
     */
    fun decompile(condition: CompiledCondition): String {
        return when (condition) {
            is CompiledCondition.BlockStateCheck ->
                "query.block_state('${condition.propertyName}') ${condition.operator} '${condition.expectedValue}'"

            is CompiledCondition.NeighborTagCheck -> {
                val mode = if (condition.matchAll) "all" else "any"
                val tags = condition.tags.joinToString(", ") { "'$it'" }
                "query.block_neighbor_has_${mode}_tags(${condition.offsetX}, ${condition.offsetY}, ${condition.offsetZ}, $tags)"
            }

            is CompiledCondition.And ->
                "(${decompile(condition.left)} && ${decompile(condition.right)})"

            is CompiledCondition.Or ->
                "(${decompile(condition.left)} || ${decompile(condition.right)})"

            is CompiledCondition.Not ->
                "!(${decompile(condition.inner)})"

            CompiledCondition.AlwaysTrue -> "true"
            CompiledCondition.AlwaysFalse -> "false"
        }
    }
}
