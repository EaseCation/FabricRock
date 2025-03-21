/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.joml.Vector2d
import org.joml.Vector3d

@Environment(value = EnvType.CLIENT)
class ModelCuboidData(private val name: String?, textureX: Double, textureY: Double, private val faceUV: ModelPart.FaceUV?, offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double, extra: Dilation, private val mirror: Boolean, textureScaleX: Double, textureScaleY: Double) {
    private val offset = Vector3d(offsetX, offsetY, offsetZ)
    private val dimensions = Vector3d(sizeX, sizeY, sizeZ)
    private val extraSize = extra
    private val textureUV = Vector2d(textureX, textureY)
    private val textureScale = Vector2d(textureScaleX, textureScaleY)

    fun createCuboid(textureWidth: Int, textureHeight: Int): ModelPart.Cuboid {
        return ModelPart.Cuboid(
            textureUV.x.toInt(),
            textureUV.y.toInt(),
            faceUV,
            offset.x,
            offset.y,
            offset.z,
            dimensions.x,
            dimensions.y,
            dimensions.z,
            extraSize.radiusX,
            extraSize.radiusY,
            extraSize.radiusZ,
            this.mirror,
            textureWidth.toDouble() * textureScale.x,
            textureHeight.toDouble() * textureScale.y
        )
    }
}

