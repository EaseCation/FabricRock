package net.easecation.bedrockloader.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/**
 * ModMenu集成类
 * 为Bedrock Loader提供配置界面入口
 */
class ModMenuIntegration : ModMenuApi {

    /**
     * 提供配置屏幕工厂
     * 在ModMenu的模组列表中点击"配置"按钮时会调用此方法
     */
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            ConfigScreen.create(parent)
        }
    }
}
