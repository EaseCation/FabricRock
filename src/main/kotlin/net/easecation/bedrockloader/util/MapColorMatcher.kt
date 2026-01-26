package net.easecation.bedrockloader.util

import net.minecraft.block.MapColor
import java.lang.reflect.Modifier

/**
 * 将自定义 RGB 颜色匹配到最接近的 Minecraft MapColor
 *
 * Minecraft 的 MapColor 系统有以下限制:
 * - MapColor.COLORS 数组大小为 64
 * - 当前版本 (1.20.6) 使用了 62 个基础颜色
 * - MapColor 构造函数是私有的，无法安全添加新颜色
 *
 * 因此，我们使用欧几里得距离算法找到最接近的现有 MapColor
 */
object MapColorMatcher {

    /**
     * 所有可用的 MapColor（排除透明色）
     * 使用 lazy 延迟初始化，通过反射获取所有静态 MapColor 字段
     */
    private val MAP_COLORS: List<MapColor> by lazy {
        // 通过反射获取 MapColor 类中所有静态 MapColor 字段
        MapColor::class.java.declaredFields
            .filter { field ->
                field.type == MapColor::class.java &&
                Modifier.isStatic(field.modifiers) &&
                Modifier.isPublic(field.modifiers)
            }
            .mapNotNull { field ->
                try {
                    field.get(null) as? MapColor
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.color != 0 }  // 排除透明色 (CLEAR)
    }

    /**
     * 找到与目标 RGB 颜色最接近的 MapColor
     *
     * @param targetRgb 目标颜色 (0xRRGGBB 格式)
     * @return 最接近的 MapColor
     */
    fun findClosestMapColor(targetRgb: Int): MapColor {
        val targetR = (targetRgb shr 16) and 0xFF
        val targetG = (targetRgb shr 8) and 0xFF
        val targetB = targetRgb and 0xFF

        return MAP_COLORS.minByOrNull { mapColor ->
            val r = (mapColor.color shr 16) and 0xFF
            val g = (mapColor.color shr 8) and 0xFF
            val b = mapColor.color and 0xFF

            // 使用欧几里得距离的平方（无需开方，仅用于比较）
            val dr = targetR - r
            val dg = targetG - g
            val db = targetB - b
            dr * dr + dg * dg + db * db
        } ?: MapColor.STONE_GRAY  // 默认返回石头灰色
    }
}
