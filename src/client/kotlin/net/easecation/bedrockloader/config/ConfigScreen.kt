package net.easecation.bedrockloader.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionFlag
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import net.easecation.bedrockloader.sync.client.ClientConfig
import net.easecation.bedrockloader.sync.client.ClientConfigLoader
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.io.File

/**
 * YACL配置屏幕构建器
 */
object ConfigScreen {

    /**
     * 获取配置文件路径
     */
    private fun getConfigFile(): File {
        val gameDir = FabricLoader.getInstance().gameDir.toFile()
        return File(gameDir, "config/bedrock-loader/client.yml")
    }

    /**
     * 创建配置屏幕
     */
    fun create(parent: Screen?): Screen {
        val configFile = getConfigFile()
        val config = ClientConfigLoader.loadClientConfig(configFile)

        // 使用可变变量来存储配置值
        var enabled = config.enabled
        var serverUrl = config.serverUrl
        var timeoutSeconds = config.timeoutSeconds
        var showUI = config.showUI
        var autoCancelOnError = config.autoCancelOnError
        var autoCleanupRemovedPacks = config.autoCleanupRemovedPacks

        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("Bedrock Loader 配置"))
            .category(
                ConfigCategory.createBuilder()
                    .name(Text.literal("远程同步设置"))
                    // 启用远程同步
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.literal("启用远程同步"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("启动时从服务器同步资源包\n"),
                                    Text.literal("启用：自动检查服务器并下载资源包\n"),
                                    Text.literal("禁用：仅使用本地资源包")
                                )
                            )
                            .binding(
                                config.enabled,
                                { enabled },
                                { enabled = it }
                            )
                            .controller { option ->
                                BooleanControllerBuilder.create(option)
                                    .formatValue { value -> Text.literal(if (value) "启用" else "禁用") }
                            }
                            .flag(OptionFlag.GAME_RESTART)
                            .build()
                    )
                    // 服务器URL
                    .option(
                        Option.createBuilder<String>()
                            .name(Text.literal("服务器URL"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("远程服务器的地址（包含协议和端口）\n"),
                                    Text.literal("示例: http://192.168.1.100:8080")
                                )
                            )
                            .binding(
                                config.serverUrl,
                                { serverUrl },
                                { serverUrl = it }
                            )
                            .controller { option ->
                                StringControllerBuilder.create(option)
                            }
                            .flag(OptionFlag.GAME_RESTART)
                            .build()
                    )
                    // 超时时间
                    .option(
                        Option.createBuilder<Int>()
                            .name(Text.literal("超时时间"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("HTTP请求超时时间（秒）\n"),
                                    Text.literal("建议值: 5-30秒")
                                )
                            )
                            .binding(
                                config.timeoutSeconds,
                                { timeoutSeconds },
                                { timeoutSeconds = it }
                            )
                            .controller { option ->
                                IntegerSliderControllerBuilder.create(option)
                                    .range(5, 60)
                                    .step(5)
                                    .formatValue { value -> Text.literal("${value}秒") }
                            }
                            .build()
                    )
                    // 显示UI
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.literal("显示同步UI"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("同步时是否显示进度界面\n"),
                                    Text.literal("启用：显示下载进度界面\n"),
                                    Text.literal("禁用：后台静默同步")
                                )
                            )
                            .binding(
                                config.showUI,
                                { showUI },
                                { showUI = it }
                            )
                            .controller { option ->
                                BooleanControllerBuilder.create(option)
                                    .formatValue { value -> Text.literal(if (value) "显示" else "隐藏") }
                            }
                            .build()
                    )
                    // 错误时自动取消
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.literal("错误时自动取消"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("发生错误时是否自动取消同步\n"),
                                    Text.literal("启用：任何错误都取消同步，使用本地包\n"),
                                    Text.literal("禁用：尝试继续同步其他文件")
                                )
                            )
                            .binding(
                                config.autoCancelOnError,
                                { autoCancelOnError },
                                { autoCancelOnError = it }
                            )
                            .controller { option ->
                                BooleanControllerBuilder.create(option)
                                    .formatValue { value -> Text.literal(if (value) "是" else "否") }
                            }
                            .build()
                    )
                    // 自动清理已删除的包
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Text.literal("自动清理已删除的包"))
                            .description(
                                OptionDescription.of(
                                    Text.literal("是否自动删除远程服务器已删除的包\n"),
                                    Text.literal("启用：自动删除remote/目录中服务器已删除的包\n"),
                                    Text.literal("禁用：保留所有本地包，不自动清理\n"),
                                    Text.literal("注意：仅清理remote/目录，手动放置的包不受影响")
                                )
                            )
                            .binding(
                                config.autoCleanupRemovedPacks,
                                { autoCleanupRemovedPacks },
                                { autoCleanupRemovedPacks = it }
                            )
                            .controller { option ->
                                BooleanControllerBuilder.create(option)
                                    .formatValue { value -> Text.literal(if (value) "是" else "否") }
                            }
                            .build()
                    )
                    .build()
            )
            .save {
                // 保存配置到YAML文件
                val newConfig = ClientConfig(
                    enabled = enabled,
                    serverUrl = serverUrl,
                    timeoutSeconds = timeoutSeconds,
                    showUI = showUI,
                    autoCancelOnError = autoCancelOnError,
                    autoCleanupRemovedPacks = autoCleanupRemovedPacks
                )
                ClientConfigLoader.saveConfig(newConfig, configFile)
            }
            .build()
            .generateScreen(parent)
    }
}
