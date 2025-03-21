/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
object ModelUtil {
    fun interpolateAngle(angle1: Double, angle2: Double, progress: Double): Double {
        var f = angle2 - angle1
        while (f < (-Math.PI)) {
            f += Math.PI * 2
        }
        while (f >= Math.PI) {
            f -= Math.PI * 2
        }
        return angle1 + progress * f
    }
}

