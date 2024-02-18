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
    private var mirror = false

    fun uv(textureX: Int, textureY: Int): ModelPartBuilder {
        this.textureX = textureX
        this.textureY = textureY
        return this
    }

    @JvmOverloads
    fun mirrored(mirror: Boolean = true): ModelPartBuilder {
        this.mirror = mirror
        return this
    }

    fun cuboid(name: String?, offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Int, sizeY: Int, sizeZ: Int, extra: Dilation, textureX: Int, textureY: Int): ModelPartBuilder {
        this.uv(textureX, textureY)
        cuboidData.add(ModelCuboidData(name, this.textureX.toFloat(), this.textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX.toFloat(), sizeY.toFloat(), sizeZ.toFloat(), extra, this.mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(name: String?, offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Int, sizeY: Int, sizeZ: Int, textureX: Int, textureY: Int): ModelPartBuilder {
        this.uv(textureX, textureY)
        cuboidData.add(ModelCuboidData(name, this.textureX.toFloat(), this.textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX.toFloat(), sizeY.toFloat(), sizeZ.toFloat(), Dilation.Companion.NONE, this.mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.Companion.NONE, this.mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(name: String?, offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(name, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.Companion.NONE, this.mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(name: String?, offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float, extra: Dilation): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(name, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float, mirror: Boolean): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, Dilation.Companion.NONE, mirror, 1.0f, 1.0f))
        return this
    }

    fun cuboid(offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float, extra: Dilation, textureScaleX: Float, textureScaleY: Float): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, textureScaleX, textureScaleY))
        return this
    }

    fun cuboid(offsetX: Float, offsetY: Float, offsetZ: Float, sizeX: Float, sizeY: Float, sizeZ: Float, extra: Dilation): ModelPartBuilder {
        cuboidData.add(ModelCuboidData(null, textureX.toFloat(), textureY.toFloat(), offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, extra, this.mirror, 1.0f, 1.0f))
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

