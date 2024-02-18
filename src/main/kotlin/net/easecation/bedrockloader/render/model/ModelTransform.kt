/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
class ModelTransform private constructor(val pivotX: Float, val pivotY: Float, val pivotZ: Float, val pitch: Float, val yaw: Float, val roll: Float) {
    companion object {
        val NONE: ModelTransform = of(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        fun pivot(pivotX: Float, pivotY: Float, pivotZ: Float): ModelTransform {
            return of(pivotX, pivotY, pivotZ, 0.0f, 0.0f, 0.0f)
        }

        fun rotation(pitch: Float, yaw: Float, roll: Float): ModelTransform {
            return of(0.0f, 0.0f, 0.0f, pitch, yaw, roll)
        }

        fun of(pivotX: Float, pivotY: Float, pivotZ: Float, pitch: Float, yaw: Float, roll: Float): ModelTransform {
            return ModelTransform(pivotX, pivotY, pivotZ, pitch, yaw, roll)
        }
    }
}

