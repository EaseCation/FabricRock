package net.easecation.bedrockloader.sync.client.ui

import java.awt.Component
import java.awt.Window
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.max

/**
 * UI工具类
 * 提供格式化、样式等辅助功能
 */
object UIUtil {
    /**
     * 格式化字节数为人类可读格式
     *
     * @param bytes 字节数
     * @return 格式化后的字符串（如 "1.5 MB"）
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * 格式化下载速度
     *
     * @param bytesPerSecond 每秒字节数
     * @return 格式化后的速度字符串（如 "2.5 MB/s"）
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 0 -> "0 B/s"
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            bytesPerSecond < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
            else -> String.format("%.2f GB/s", bytesPerSecond / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * 格式化时间为人类可读格式
     *
     * @param seconds 总秒数
     * @return 格式化后的时间字符串（如 "1:30" 或 "2:15:45"）
     */
    fun formatTime(seconds: Long): String {
        if (seconds < 0) return "未知"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> "${secs}秒"
        }
    }

    /**
     * 居中显示窗口
     *
     * @param window 要居中的窗口
     */
    fun centerWindow(window: Window) {
        window.setLocationRelativeTo(null)
    }

    /**
     * 检查并设置macOS兼容性
     * 在macOS上显示Swing窗口前调用
     */
    fun ensureMacOSCompatibility() {
        if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
            System.setProperty("apple.awt.UIElement", "false")
        }
    }

    /**
     * 将窗口置于顶层并获取焦点
     * 使用增强型方法确保跨平台兼容性
     *
     * @param window 要置顶的窗口
     */
    fun bringToFront(window: Window) {
        SwingUtilities.invokeLater {
            // 如果窗口被最小化，先恢复
            if (window is JFrame) {
                if (window.extendedState and JFrame.ICONIFIED != 0) {
                    window.extendedState = window.extendedState and JFrame.ICONIFIED.inv()
                }
            }

            // 临时置顶 → 置前 → 请求焦点 → 取消置顶
            window.isAlwaysOnTop = true
            window.toFront()
            window.requestFocus()
            window.isAlwaysOnTop = false
        }
    }

    /**
     * 计算进度百分比
     *
     * @param current 当前值
     * @param total 总值
     * @return 百分比（0-100）
     */
    fun calculatePercent(current: Long, total: Long): Int {
        if (total <= 0) return 0
        return max(0, (current * 100 / total).toInt().coerceAtMost(100))
    }

    /**
     * 计算剩余时间
     *
     * @param remainingBytes 剩余字节数
     * @param bytesPerSecond 每秒字节数
     * @return 剩余秒数
     */
    fun calculateRemainingTime(remainingBytes: Long, bytesPerSecond: Long): Long {
        if (bytesPerSecond <= 0) return -1
        return remainingBytes / bytesPerSecond
    }
}
