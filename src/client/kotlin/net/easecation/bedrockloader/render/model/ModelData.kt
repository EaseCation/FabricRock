/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import com.google.common.collect.ImmutableList
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
class ModelData {
    val root: ModelPartData = ModelPartData(ImmutableList.of<ModelCuboidData?>(), ModelTransform.NONE)
}

