/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
class TexturedModelData private constructor(private val data: ModelData, private val dimensions: TextureDimensions) {
    fun createModel(): ModelPart {
        return data.root.createPart(dimensions.width, dimensions.height)
    }

    companion object {
        fun of(partData: ModelData, textureWidth: Int, textureHeight: Int): TexturedModelData {
            return TexturedModelData(partData, TextureDimensions(textureWidth, textureHeight))
        }
    }
}

