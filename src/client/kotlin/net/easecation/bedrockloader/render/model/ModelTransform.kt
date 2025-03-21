/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
class ModelTransform private constructor(val pivotX: Double, val pivotY: Double, val pivotZ: Double, val pitch: Double, val yaw: Double, val roll: Double, val inflate: Double, val detachPivot: Boolean = false) {
    companion object {
        val NONE: ModelTransform = of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        fun pivot(pivotX: Double, pivotY: Double, pivotZ: Double): ModelTransform {
            return of(pivotX, pivotY, pivotZ, 0.0, 0.0, 0.0, 0.0)
        }

        fun rotation(pitch: Double, yaw: Double, roll: Double): ModelTransform {
            return of(0.0, 0.0, 0.0, pitch, yaw, roll, 0.0)
        }

        fun of(pivotX: Double, pivotY: Double, pivotZ: Double, pitch: Double, yaw: Double, roll: Double, inflate: Double, detachPivot: Boolean = false): ModelTransform {
            return ModelTransform(pivotX, pivotY, pivotZ, pitch, yaw, roll, inflate, detachPivot)
        }
    }
}

