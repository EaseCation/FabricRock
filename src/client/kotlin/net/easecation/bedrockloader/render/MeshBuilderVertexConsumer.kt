package net.easecation.bedrockloader.render

//? if <1.21.4 {
/*import net.fabricmc.fabric.api.renderer.v1.Renderer
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

    override fun vertex(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.pos(vertexIndex, x, y, z)
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        emitter.uv(vertexIndex, u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        return this
    }

    override fun overlay(overlay: Int): VertexConsumer {
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, v)
        return this
    }

    override fun light(light: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, light)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.normal(vertexIndex, x, y, z)
        return this
    }

    /^*
     * 1.21.1 组合 vertex 方法 (11 参数, int color 替代 4 float RGBA)
     * 这是自定义 ModelPart 的 Cuboid.renderCuboid() 调用路径
     ^/
    override fun vertex(
        x: Float, y: Float, z: Float,
        color: Int,
        u: Float, v: Float,
        overlay: Int, light: Int,
        normalX: Float, normalY: Float, normalZ: Float
    ) {
        emitter.pos(vertexIndex, x, y, z)
        emitter.color(vertexIndex, color)
        emitter.uv(vertexIndex, u, v)
        emitter.lightmap(vertexIndex, light)
        emitter.normal(vertexIndex, normalX, normalY, normalZ)
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            vertexIndex = 0
        }
    }

    fun build(): Mesh {
        return meshBuilder.build()
    }
}
*///?} else {
import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.Sprite

class MeshBuilderVertexConsumer(private val defaultSprite: Sprite, private val sprites: Map<String, Sprite>) : VertexConsumer {

    private val renderer: Renderer = Renderer.get()
    private val mutableMesh: MutableMesh = renderer.mutableMesh()
    private val emitter = mutableMesh.emitter()

    private var vertexIndex = 0
    private var material: String? = null

    fun material(material: String?) {
        this.material = material
    }

    override fun vertex(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.pos(vertexIndex, x, y, z)
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
        return this
    }

    //? if >=1.21.11 {
    override fun color(color: Int): VertexConsumer {
        emitter.color(vertexIndex, color)
        return this
    }

    override fun lineWidth(width: Float): VertexConsumer {
        return this
    }
    //?}

    override fun texture(u: Float, v: Float): VertexConsumer {
        emitter.uv(vertexIndex, u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        return this
    }

    override fun overlay(overlay: Int): VertexConsumer {
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, v)
        return this
    }

    override fun light(light: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, light)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.normal(vertexIndex, x, y, z)
        return this
    }

    override fun vertex(
        x: Float, y: Float, z: Float,
        color: Int,
        u: Float, v: Float,
        overlay: Int, light: Int,
        normalX: Float, normalY: Float, normalZ: Float
    ) {
        emitter.pos(vertexIndex, x, y, z)
        emitter.color(vertexIndex, color)
        emitter.uv(vertexIndex, u, v)
        emitter.lightmap(vertexIndex, light)
        emitter.normal(vertexIndex, normalX, normalY, normalZ)
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            vertexIndex = 0
        }
    }

    fun build(): Mesh {
        return mutableMesh.immutableCopy()
    }
}
//?}
