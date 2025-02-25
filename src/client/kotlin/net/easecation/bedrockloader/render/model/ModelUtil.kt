/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
object ModelUtil {
    fun interpolateAngle(angle1: Float, angle2: Float, progress: Float): Float {
        var f = angle2 - angle1
        while (f < (-Math.PI).toFloat()) {
            f += Math.PI.toFloat() * 2
        }
        while (f >= Math.PI.toFloat()) {
            f -= Math.PI.toFloat() * 2
        }
        return angle1 + progress * f
    }
}

