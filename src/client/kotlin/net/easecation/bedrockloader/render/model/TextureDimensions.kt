/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * Internal class used by [TexturedModelData].
 */
@Environment(value = EnvType.CLIENT)
class TextureDimensions(val width: Int, val height: Int)

