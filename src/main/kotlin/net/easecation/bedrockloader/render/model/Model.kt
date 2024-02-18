/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import java.util.function.Function

/**
 * Represents a dynamic model which has its own render layers and custom rendering.
 */
@Environment(value = EnvType.CLIENT)
abstract class Model(protected val layerFactory: Function<Identifier, RenderLayer>) {
    /**
     * {@return the render layer for the corresponding texture}
     *
     * @param texture the texture used for the render layer
     */
    fun getLayer(texture: Identifier): RenderLayer {
        return layerFactory.apply(texture)
    }

    /**
     * Renders the model.
     *
     * @param light the lightmap coordinates used for this model rendering
     */
    abstract fun render(var1: MatrixStack?, var2: VertexConsumer?, var3: Int, var4: Int, var5: Float, var6: Float, var7: Float, var8: Float)
}

