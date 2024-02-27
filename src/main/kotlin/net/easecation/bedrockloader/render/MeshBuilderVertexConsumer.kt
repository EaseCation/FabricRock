package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.BedrockLoader
import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3f
import net.minecraft.util.math.Vector4f
import org.lwjgl.system.MemoryStack

class MeshBuilderVertexConsumer : VertexIndexedVertexConsumer {

    private val renderer: Renderer = RendererAccess.INSTANCE.renderer!!
    private val meshBuilder: MeshBuilder = renderer.meshBuilder()
    private val emitter = meshBuilder.emitter

    private var vertexIndex = 0

    override fun quad(matrixEntry: MatrixStack.Entry, quad: BakedQuad, brightnesses: FloatArray, red: Float, green: Float, blue: Float, lights: IntArray, overlay: Int, useQuadColorData: Boolean) {
        val fs = floatArrayOf(brightnesses[0], brightnesses[1], brightnesses[2], brightnesses[3])
        val `is` = intArrayOf(lights[0], lights[1], lights[2], lights[3])
        val js = quad.vertexData
        val vec3i = quad.face.vector
        val vec3f = Vec3f(vec3i.x.toFloat(), vec3i.y.toFloat(), vec3i.z.toFloat())
        val matrix4f = matrixEntry.positionMatrix
        vec3f.transform(matrixEntry.normalMatrix)
        val i = 8
        val j = js.size / 8
        MemoryStack.stackPush().use { memoryStack ->
            val byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.vertexSize)
            val intBuffer = byteBuffer.asIntBuffer()
            for (k in 0 until j) {
                var q: Float
                var p: Float
                var o: Float
                var n: Float
                var m: Float
                intBuffer.clear()
                intBuffer.put(js, k * 8, 8)
                val f = byteBuffer.getFloat(0)
                val g = byteBuffer.getFloat(4)
                val h = byteBuffer.getFloat(8)
                if (useQuadColorData) {
                    val l = (byteBuffer[12].toInt() and 0xFF).toFloat() / 255.0f
                    m = (byteBuffer[13].toInt() and 0xFF).toFloat() / 255.0f
                    n = (byteBuffer[14].toInt() and 0xFF).toFloat() / 255.0f
                    o = l * fs[k] * red
                    p = m * fs[k] * green
                    q = n * fs[k] * blue
                } else {
                    o = fs[k] * red
                    p = fs[k] * green
                    q = fs[k] * blue
                }
                val r = `is`[k]
                m = byteBuffer.getFloat(16)
                n = byteBuffer.getFloat(20)
                val vector4f = Vector4f(f, g, h, 1.0f)
                vector4f.transform(matrix4f)
                emitter.pos(k, vector4f.x, vector4f.y, vector4f.z)
                // emitter.spriteBake(k, )  // TODO texture
                emitter.spriteColor(k, -1, -1, -1, -1)
                emitter.spriteColor(k, (o * 255).toInt(), (p * 255).toInt(), (q * 255).toInt(), 255)
                emitter.normal(k, vec3f)
                emitter.lightmap(k, r)
                this.vertex(vector4f.x, vector4f.y, vector4f.z, o, p, q, 1.0f, m, n, overlay, r, vec3f.x, vec3f.y, vec3f.z)
                emitter.emit()
            }
        }
    }

    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.vertex($x, $y, $z)")
        emitter.pos(vertexIndex, x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.color($red, $green, $blue, $alpha)")
        emitter.spriteColor(0, red, green, blue, alpha)
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.texture($u, $v)")
        emitter.sprite(vertexIndex, 0, u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($u, $v)")
        return this
    }

    override fun overlay(overlay: Int): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($overlay)")
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($u, $v)")
        emitter.lightmap(vertexIndex, v)
        return this
    }

    override fun light(light: Int): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($light)")
        emitter.lightmap(vertexIndex, light)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.normal($x, $y, $z)")
        emitter.normal(vertexIndex, x, y, z)
        return this
    }

    override fun next() {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.next()")
        emitter.emit()
    }

    override fun vertexIndex(index: Int) {
        vertexIndex = index
    }

    override fun fixedColor(red: Int, green: Int, blue: Int, alpha: Int) {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.fixedColor($red, $green, $blue, $alpha)")
        emitter.spriteColor(vertexIndex, red, green, blue, alpha)
    }

    override fun unfixColor() {
        BedrockLoader.logger.info("MeshBuilderVertexConsumer.unfixColor()")
        emitter.spriteColor(vertexIndex, -1, -1, -1, -1)
    }

    fun build(): Mesh {
        return meshBuilder.build()
    }
}