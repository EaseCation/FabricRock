/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * @implNote This should be in same package as [ModelCuboidData] as
 * its package private static fields are accessed by it.
 */
@Environment(value = EnvType.CLIENT)
class Dilation(val radiusX: Float, val radiusY: Float, val radiusZ: Float) {
    constructor(radius: Float) : this(radius, radius, radius)

    fun add(radius: Float): Dilation {
        return Dilation(this.radiusX + radius, this.radiusY + radius, this.radiusZ + radius)
    }

    fun add(radiusX: Float, radiusY: Float, radiusZ: Float): Dilation {
        return Dilation(this.radiusX + radiusX, this.radiusY + radiusY, this.radiusZ + radiusZ)
    }

    companion object {
        val NONE: Dilation = Dilation(0.0f)
    }
}

