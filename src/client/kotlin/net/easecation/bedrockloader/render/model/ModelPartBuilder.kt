/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
class ModelPartBuilder {
    private val cuboidData: MutableList<ModelCuboidData> = Lists.newArrayList()
    private var textureX = 0
    private var textureY = 0
    private var faceUV: ModelPart.FaceUV? = null
    private var mirror = false

    fun uv(faceUV: ModelPart.FaceUV): ModelPartBuilder {
        this.textureX = 0
        this.textureY = 0
        this.faceUV = faceUV
        return this
    }

    fun uv(textureX: Int, textureY: Int): ModelPartBuilder {
        this.textureX = textureX
        this.textureY = textureY
        this.faceUV = null
        return this
    }

    @JvmOverloads
    fun mirrored(mirror: Boolean = true): ModelPartBuilder {
        this.mirror = mirror
        return this
    }

    fun cuboid(name: String?, offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Int, sizeY: Int, sizeZ: Int, extra: Dilation, textureX: Int, textureY: Int): ModelPartBuilder {
        this.uv(textureX, textureY)
        cuboidData.add(ModelCuboidData(name, this.textureX.toDouble(), this.textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX.toDouble(), sizeY.toDouble(), sizeZ.toDouble(), extra, this.mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(name: String?, offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Int, sizeY: Int, sizeZ: Int, textureX: Int, textureY: Int): ModelPartBuilder {
        this.uv(textureX, textureY)
        cuboidData.add(ModelCuboidData(name, this.textureX.toDouble(), this.textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX.toDouble(), sizeY.toDouble(), sizeZ.toDouble(), Dilation.NONE, this.mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.NONE, this.mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(name: String?, offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(name, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.NONE, this.mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(name: String?, offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double, extra: Dilation): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(name, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double, mirror: Boolean): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.NONE, mirror, 1.0, 1.0))
        return this
    }

    fun cuboid(offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double, extra: Dilation, textureScaleX: Double, textureScaleY: Double): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, textureScaleX, textureScaleY))
        return this
    }

    fun cuboid(offsetX: Double, offsetY: Double, offsetZ: Double, sizeX: Double, sizeY: Double, sizeZ: Double, extra: Dilation): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toDouble(), textureY.toDouble(), faceUV, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, 1.0, 1.0))
        return this
    }

    fun build(): List<ModelCuboidData> {
        return ImmutableList.copyOf(this.cuboidData)
    }

    companion object {
        fun create(): ModelPartBuilder {
            return ModelPartBuilder()
        }
    }
}

