package net.easecation.bedrockloader.render

import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.Sprite

class MeshBuilderVertexConsumer(private val defaultSprite: Sprite, private val sprites: Map<String, Sprite>) : VertexConsumer {

    private val renderer: Renderer = RendererAccess.INSTANCE.renderer!!
    private val meshBuilder: MeshBuilder = renderer.meshBuilder()
    private val emitter = meshBuilder.emitter

    private var vertexIndex = 0
    private var material: String? = null

    fun material(material: String?) {
        this.material = material
    }

    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.vertex($x, $y, $z)")
        emitter.pos(vertexIndex, x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.color($red, $green, $blue, $alpha)")
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.texture($u, $v)")
        emitter.uv(vertexIndex, u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($u, $v)")
        return this
    }

    override fun overlay(overlay: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($overlay)")
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($u, $v)")
        emitter.lightmap(vertexIndex, v)
        return this
    }

    override fun light(light: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($light)")
        emitter.lightmap(vertexIndex, light)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.normal($x, $y, $z)")
        emitter.normal(vertexIndex, x, y, z)
        return this
    }

    override fun next() {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.next()")
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            vertexIndex = 0
        }
    }

    override fun fixedColor(red: Int, green: Int, blue: Int, alpha: Int) {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.fixedColor($red, $green, $blue, $alpha)")
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
    }

    override fun unfixColor() {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.unfixColor()")
        emitter.color(vertexIndex, -1)
    }

    fun build(): Mesh {
        return meshBuilder.build()
    }
}