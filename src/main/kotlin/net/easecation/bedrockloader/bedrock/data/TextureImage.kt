package net.easecation.bedrockloader.bedrock.data

import java.awt.image.BufferedImage

data class TextureImage(
        val image: BufferedImage,
        val type: ImageType
) {

    constructor(image: BufferedImage, type: String) : this(image, ImageType.valueOf(type.uppercase()))

    enum class ImageType {
        PNG,
        JPG,
        TGA;
        fun getExtension(): String {
            return name.lowercase()
        }
    }
}
