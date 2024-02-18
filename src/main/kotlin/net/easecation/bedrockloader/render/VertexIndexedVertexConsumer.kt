package net.easecation.bedrockloader.render

import net.minecraft.client.render.VertexConsumer

interface VertexIndexedVertexConsumer : VertexConsumer {

    fun vertexIndex(index: Int)

}