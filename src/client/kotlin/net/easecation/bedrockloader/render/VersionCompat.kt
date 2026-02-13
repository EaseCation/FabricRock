package net.easecation.bedrockloader.render

import net.minecraft.util.Identifier

/**
 * 版本兼容性辅助类
 * 处理不同Minecraft版本间的API差异
 */
object VersionCompat {
    /**
     * 获取方块纹理集(Block Atlas)的标识符
     *
     * 在1.21.1-1.21.3中,使用PlayerScreenHandler.BLOCK_ATLAS_TEXTURE
     * 在1.21.4+中,直接使用Identifier
     */
    //? if >=1.21.4 {
    val BLOCK_ATLAS_TEXTURE: Identifier = Identifier.of("minecraft", "textures/atlas/blocks.png")
    //?} else {
    /*val BLOCK_ATLAS_TEXTURE: Identifier = net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE
    *///?}
}
