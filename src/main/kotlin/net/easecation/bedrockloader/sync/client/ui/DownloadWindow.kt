package net.easecation.bedrockloader.sync.client.ui

import net.easecation.bedrockloader.sync.client.model.SyncError
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * 下载进度窗口
 * 显示同步和下载进度的Swing窗口
 */
class DownloadWindow(
    private val onCancel: () -> Unit
) : JFrame("Bedrock Loader - 资源包同步") {

    // UI组件
    private val statusLabel: JLabel = JLabel("初始化...")
    private val progressBar: JProgressBar = JProgressBar(0, 100)
    private val detailsLabel: JLabel = JLabel(" ")
    private val currentFileLabel: JLabel = JLabel(" ")
    private val cancelButton: JButton = JButton("取消")

    // 状态标志
    private var isFinished = false

    init {
        // 窗口设置
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        setSize(500, 220)
        isResizable = false
        UIUtil.centerWindow(this)

        // 创建主面板
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

        // 创建内容面板
        val contentPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = Insets(5, 0, 5, 0)

        // 状态标签
        statusLabel.font = statusLabel.font.deriveFont(12f)
        contentPanel.add(statusLabel, gbc)

        // 进度条
        gbc.gridy++
        progressBar.isStringPainted = true
        progressBar.string = "0%"
        contentPanel.add(progressBar, gbc)

        // 详情标签（已下载/总大小 | 速度）
        gbc.gridy++
        detailsLabel.font = detailsLabel.font.deriveFont(10f)
        contentPanel.add(detailsLabel, gbc)

        // 当前文件标签
        gbc.gridy++
        currentFileLabel.font = currentFileLabel.font.deriveFont(10f)
        contentPanel.add(currentFileLabel, gbc)

        mainPanel.add(contentPanel, BorderLayout.CENTER)

        // 创建按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        cancelButton.addActionListener {
            handleCancel()
        }
        buttonPanel.add(cancelButton)

        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        // 添加到窗口
        contentPane = mainPanel

        // 窗口关闭事件
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                handleCancel()
            }
        })
    }

    /**
     * 更新状态文本
     *
     * @param status 状态信息
     */
    fun updateStatus(status: String) {
        SwingUtilities.invokeLater {
            statusLabel.text = "状态: $status"
        }
    }

    /**
     * 更新进度条
     *
     * @param percent 进度百分比（0-100）
     */
    fun updateProgress(percent: Int) {
        SwingUtilities.invokeLater {
            val clampedPercent = percent.coerceIn(0, 100)
            progressBar.value = clampedPercent
            progressBar.string = "$clampedPercent%"
        }
    }

    /**
     * 更新详细信息
     *
     * @param currentFile 当前文件名
     * @param downloaded 已下载字节数
     * @param total 总字节数
     * @param speed 下载速度（字节/秒）
     */
    fun updateDetails(currentFile: String, downloaded: Long, total: Long, speed: Long) {
        SwingUtilities.invokeLater {
            // 格式化详情信息
            val downloadedStr = UIUtil.formatBytes(downloaded)
            val totalStr = UIUtil.formatBytes(total)
            val speedStr = UIUtil.formatSpeed(speed)

            detailsLabel.text = "已下载: $downloadedStr / $totalStr | 速度: $speedStr"
            currentFileLabel.text = "当前文件: $currentFile"
        }
    }

    /**
     * 更新当前文件
     *
     * @param fileName 文件名
     */
    fun updateCurrentFile(fileName: String) {
        SwingUtilities.invokeLater {
            currentFileLabel.text = "当前文件: $fileName"
        }
    }

    /**
     * 清空详细信息
     */
    fun clearDetails() {
        SwingUtilities.invokeLater {
            detailsLabel.text = " "
            currentFileLabel.text = " "
        }
    }

    /**
     * 显示错误信息并提供选择
     *
     * @param error 错误对象
     * @param onContinue 点击"继续"的回调
     * @param onExit 点击"退出"的回调
     */
    fun showError(error: SyncError, onContinue: () -> Unit, onExit: () -> Unit) {
        SwingUtilities.invokeLater {
            // 隐藏当前窗口
            isVisible = false

            // 显示错误对话框
            ErrorDialog.show(this, error, onContinue, onExit)
        }
    }

    /**
     * 显示成功信息
     */
    fun showSuccess() {
        SwingUtilities.invokeLater {
            updateStatus("同步完成")
            updateProgress(100)
            clearDetails()
            cancelButton.isEnabled = false
        }
    }

    /**
     * 切换到关闭模式
     * 将取消按钮改为关闭按钮，允许用户直接关闭窗口
     */
    fun switchToCloseMode() {
        isFinished = true
        SwingUtilities.invokeLater {
            cancelButton.text = "关闭"
            cancelButton.isEnabled = true
        }
    }

    /**
     * 关闭窗口
     */
    fun close() {
        SwingUtilities.invokeLater {
            dispose()
        }
    }

    /**
     * 将窗口置于顶层并获取焦点
     */
    fun bringToFront() {
        UIUtil.bringToFront(this)
    }

    /**
     * 处理取消按钮点击
     */
    private fun handleCancel() {
        // 如果已完成，直接关闭窗口
        if (isFinished) {
            close()
            return
        }

        // 否则显示取消确认对话框
        val choice = JOptionPane.showConfirmDialog(
            this,
            "确定要取消同步吗？\n游戏将使用本地资源包继续启动。",
            "确认取消",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        if (choice == JOptionPane.YES_OPTION) {
            updateStatus("正在取消...")
            cancelButton.isEnabled = false
            onCancel()
        }
    }

    /**
     * 禁用取消按钮
     */
    fun disableCancelButton() {
        SwingUtilities.invokeLater {
            cancelButton.isEnabled = false
        }
    }

    /**
     * 启用取消按钮
     */
    fun enableCancelButton() {
        SwingUtilities.invokeLater {
            cancelButton.isEnabled = true
        }
    }
}
