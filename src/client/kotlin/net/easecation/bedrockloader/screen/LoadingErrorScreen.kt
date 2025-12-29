package net.easecation.bedrockloader.screen

import net.easecation.bedrockloader.loader.error.LoadingError
import net.easecation.bedrockloader.loader.error.LoadingErrorCollector
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.min

/**
 * 加载错误详情界面
 */
class LoadingErrorScreen(private val parent: Screen?) : Screen(Text.literal("加载问题")) {

    private val errors: List<LoadingError> = LoadingErrorCollector.getErrors()
    private var scrollOffset = 0
    private val itemHeight = 60 // 每个错误项的高度
    private val padding = 10
    private var expandedIndex = -1 // 当前展开的错误索引

    override fun init() {
        super.init()

        // 返回按钮
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("< 返回")) { close() }
                .dimensions(padding, padding, 60, 20)
                .build()
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 渲染背景
        this.renderBackground(context, mouseX, mouseY, delta)

        // 标题
        val titleText = "加载问题 (${errors.size})"
        context.drawCenteredTextWithShadow(textRenderer, titleText, width / 2, padding + 5, 0xFFFFFF)

        // 渲染错误列表
        val listTop = padding + 30
        val listBottom = height - padding
        val listHeight = listBottom - listTop
        val visibleItems = listHeight / itemHeight

        // 启用裁剪
        context.enableScissor(padding, listTop, width - padding, listBottom)

        var y = listTop - scrollOffset
        for ((index, error) in errors.withIndex()) {
            if (y + itemHeight > listTop - itemHeight && y < listBottom + itemHeight) {
                renderErrorItem(context, error, index, padding, y, width - padding * 2, mouseX, mouseY)
            }
            y += if (index == expandedIndex && error.exception != null) {
                itemHeight + 80 // 展开时增加高度
            } else {
                itemHeight
            }
        }

        context.disableScissor()

        // 滚动条
        val totalHeight = calculateTotalHeight()
        if (totalHeight > listHeight) {
            val scrollBarHeight = max(20, (listHeight * listHeight) / totalHeight)
            val scrollBarY = listTop + ((scrollOffset * (listHeight - scrollBarHeight)) / (totalHeight - listHeight))
            context.fill(width - padding - 6, listTop, width - padding - 2, listBottom, 0x40FFFFFF)
            context.fill(width - padding - 6, scrollBarY, width - padding - 2, scrollBarY + scrollBarHeight, 0xFFAAAAAA.toInt())
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun calculateTotalHeight(): Int {
        var total = 0
        for ((index, error) in errors.withIndex()) {
            total += if (index == expandedIndex && error.exception != null) {
                itemHeight + 80
            } else {
                itemHeight
            }
        }
        return total
    }

    private fun renderErrorItem(context: DrawContext, error: LoadingError, index: Int, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        // 背景颜色
        val bgColor = if (error.level == LoadingError.Level.ERROR) 0x40FF5555 else 0x40FFAA00
        context.fill(x, y, x + width, y + itemHeight - 2, bgColor)

        // 级别图标和文字
        val levelIcon = if (error.level == LoadingError.Level.ERROR) "X" else "!"
        val levelColor = if (error.level == LoadingError.Level.ERROR) 0xFF5555 else 0xFFAA00
        context.drawTextWithShadow(textRenderer, levelIcon, x + 5, y + 5, levelColor)

        // 级别和阶段
        val headerText = "${error.getLevelDisplayName()} - ${error.phase.displayName}"
        context.drawTextWithShadow(textRenderer, headerText, x + 20, y + 5, 0xFFFFFF)

        // 来源
        val sourceText = "来源: ${error.source}"
        context.drawTextWithShadow(textRenderer, sourceText, x + 20, y + 18, 0xAAAAAA)

        // 消息
        val messageText = error.message
        val maxMessageWidth = width - 40
        val trimmedMessage = if (textRenderer.getWidth(messageText) > maxMessageWidth) {
            var trimmed = messageText
            while (textRenderer.getWidth(trimmed + "...") > maxMessageWidth && trimmed.isNotEmpty()) {
                trimmed = trimmed.dropLast(1)
            }
            "$trimmed..."
        } else {
            messageText
        }
        context.drawTextWithShadow(textRenderer, trimmedMessage, x + 20, y + 31, 0xDDDDDD)

        // 展开/收起按钮
        val exception = error.exception
        if (exception != null) {
            val expandText = if (index == expandedIndex) "[收起详情]" else "[展开详情]"
            val expandX = x + 20
            val expandY = y + 44
            val isHovered = mouseX >= expandX && mouseX <= expandX + textRenderer.getWidth(expandText) &&
                           mouseY >= expandY && mouseY <= expandY + 10
            context.drawTextWithShadow(textRenderer, expandText, expandX, expandY, if (isHovered) 0x55FF55 else 0x55FFFF)

            // 展开的堆栈信息
            if (index == expandedIndex) {
                val stackLines = exception.stackTraceToString().lines().take(5)
                var stackY = y + itemHeight
                for (line in stackLines) {
                    val trimmedLine = if (textRenderer.getWidth(line) > width - 40) {
                        line.take(80) + "..."
                    } else {
                        line
                    }
                    context.drawTextWithShadow(textRenderer, trimmedLine, x + 25, stackY, 0x888888)
                    stackY += 12
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val listTop = padding + 30
            var y = listTop - scrollOffset

            for ((index, error) in errors.withIndex()) {
                val currentItemHeight = if (index == expandedIndex && error.exception != null) {
                    itemHeight + 80
                } else {
                    itemHeight
                }

                if (mouseY >= y && mouseY < y + currentItemHeight) {
                    // 检查是否点击了展开按钮
                    if (error.exception != null) {
                        val expandX = padding + 20
                        val expandY = y + 44
                        val expandText = if (index == expandedIndex) "[收起详情]" else "[展开详情]"
                        if (mouseX >= expandX && mouseX <= expandX + textRenderer.getWidth(expandText) &&
                            mouseY >= expandY && mouseY <= expandY + 10) {
                            expandedIndex = if (expandedIndex == index) -1 else index
                            return true
                        }
                    }
                }

                y += currentItemHeight
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listTop = padding + 30
        val listBottom = height - padding
        val listHeight = listBottom - listTop
        val totalHeight = calculateTotalHeight()
        val maxScroll = max(0, totalHeight - listHeight)

        scrollOffset = (scrollOffset - (verticalAmount * 20).toInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun close() {
        client?.setScreen(parent)
    }
}
