package net.easecation.bedrockloader.sync.client.ui

import net.easecation.bedrockloader.sync.client.model.SyncError
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * 错误对话框
 * 显示同步错误信息，并提供"继续启动"和"退出游戏"选项
 */
object ErrorDialog {
    /**
     * 显示错误对话框
     *
     * @param parent 父组件（可为null）
     * @param error 同步错误对象
     * @param onContinue 点击"继续启动"的回调
     * @param onExit 点击"退出游戏"的回调
     */
    fun show(
        parent: Component?,
        error: SyncError,
        onContinue: () -> Unit,
        onExit: () -> Unit
    ) {
        SwingUtilities.invokeLater {
            val dialog = JDialog(null as java.awt.Frame?, "同步失败", true)
            dialog.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

            // 创建主面板
            val mainPanel = JPanel(BorderLayout(10, 10))
            mainPanel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

            // 创建内容面板
            val contentPanel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.anchor = GridBagConstraints.WEST
            gbc.insets = Insets(5, 5, 5, 5)

            // 标题
            val titleLabel = JLabel("⚠️  同步失败")
            titleLabel.font = titleLabel.font.deriveFont(16f).deriveFont(java.awt.Font.BOLD)
            contentPanel.add(titleLabel, gbc)

            // 分隔线
            gbc.gridy++
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            contentPanel.add(JSeparator(), gbc)

            // 错误类型
            gbc.gridy++
            gbc.fill = GridBagConstraints.NONE
            gbc.weightx = 0.0
            val errorType = getErrorType(error)
            contentPanel.add(JLabel("错误类型: $errorType"), gbc)

            // 详细信息
            gbc.gridy++
            val detailMessage = error.message ?: "未知错误"
            val detailLabel = JLabel("<html><body style='width: 350px'>详细信息: $detailMessage</body></html>")
            contentPanel.add(detailLabel, gbc)

            // 额外信息（如果有）
            val extraInfo = getExtraInfo(error)
            if (extraInfo != null) {
                gbc.gridy++
                contentPanel.add(JLabel(extraInfo), gbc)
            }

            // 提示信息
            gbc.gridy++
            gbc.insets = Insets(15, 5, 5, 5)
            val hintLabel = JLabel("<html><body style='width: 350px'><b>你可以选择:</b><br>" +
                    "• 继续启动游戏（使用本地资源包）<br>" +
                    "• 退出游戏并修复问题</body></html>")
            contentPanel.add(hintLabel, gbc)

            mainPanel.add(contentPanel, BorderLayout.CENTER)

            // 创建按钮面板
            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 0))

            val continueButton = JButton("继续启动")
            continueButton.addActionListener {
                dialog.dispose()
                onContinue()
            }

            val exitButton = JButton("退出游戏")
            exitButton.addActionListener {
                dialog.dispose()
                onExit()
            }

            buttonPanel.add(continueButton)
            buttonPanel.add(exitButton)

            mainPanel.add(buttonPanel, BorderLayout.SOUTH)

            dialog.contentPane = mainPanel
            dialog.pack()
            dialog.setSize(450, dialog.height)
            UIUtil.centerWindow(dialog)
            dialog.isVisible = true
        }
    }

    /**
     * 根据错误类型返回错误类型字符串
     */
    private fun getErrorType(error: SyncError): String {
        return when (error) {
            is SyncError.NetworkError -> "网络连接失败"
            is SyncError.ServerError -> "服务器错误 (HTTP ${error.statusCode})"
            is SyncError.ConfigError -> "配置文件错误"
            is SyncError.FileError -> "文件操作失败"
            is SyncError.UnknownError -> "未知错误"
        }
    }

    /**
     * 获取额外信息（如文件名、状态码等）
     */
    private fun getExtraInfo(error: SyncError): String? {
        return when (error) {
            is SyncError.FileError -> "文件名: ${error.filename}"
            else -> null
        }
    }

    /**
     * 简化版错误对话框，仅显示错误信息
     * 用于快速显示错误提示
     */
    fun showSimple(parent: Component?, title: String, message: String) {
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
