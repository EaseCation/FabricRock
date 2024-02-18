/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.util.math.Vector2f
import net.minecraft.util.math.Vec3f

@Environment(value = EnvType.CLIENT)
class ModelCuboidData(private val name: String?, textureX: Float, textureY: Float, offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float, extra: Dilation, private val mirror: Boolean, textureScaleX: Float, textureScaleY: Float) {
    private val offset = Vec3f(offsetX, offsetY, offsetZ)
    private val dimensions = Vec3f(sizeX, sizeY, sizeZ)
    private val extraSize = extra
    private val textureUV = Vector2f(textureX, textureY)
    private val textureScale = Vector2f(textureScaleX, textureScaleY)

    fun createCuboid(textureWidth: Int, textureHeight: Int): ModelPart.Cuboid {
        return ModelPart.Cuboid(textureUV.x.toInt(), textureUV.y.toInt(), offset.x, offset.y, offset.z, dimensions.x, dimensions.y, dimensions.z, extraSize.radiusX, extraSize.radiusY, extraSize.radiusZ, this.mirror, textureWidth.toFloat() * textureScale.x, textureHeight.toFloat() * textureScale.y)
    }
}

