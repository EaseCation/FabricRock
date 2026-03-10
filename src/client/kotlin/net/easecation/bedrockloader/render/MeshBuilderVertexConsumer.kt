package net.easecation.bedrockloader.render

//? if <1.21 {
/*import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.Sprite

class MeshBuilderVertexConsumer(
    private val defaultSprite: Sprite,
    private val sprites: Map<String, Sprite>,
    private val doubleSidedMaterials: Set<String> = emptySet()
) : VertexConsumer {

    private val renderer: Renderer = RendererAccess.INSTANCE.renderer!!
    private val meshBuilder: MeshBuilder = renderer.meshBuilder()
    private val emitter = meshBuilder.emitter

    private var vertexIndex = 0
    private var material: String? = null
    private var currentDoubleSided = false

    private data class VertexData(
        var px: Float = 0f, var py: Float = 0f, var pz: Float = 0f,
        var color: Int = -1,
        var u: Float = 0f, var v: Float = 0f,
        var lightmap: Int = 0,
        var nx: Float = 0f, var ny: Float = 0f, var nz: Float = 0f
    )
    private val vbuf = Array(4) { VertexData() }

    fun material(material: String?) {
        this.material = material
        val key = material ?: "*"
        currentDoubleSided = key in doubleSidedMaterials || "*" in doubleSidedMaterials
    }

    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
        emitter.pos(vertexIndex, x.toFloat(), y.toFloat(), z.toFloat())
        vbuf[vertexIndex].px = x.toFloat(); vbuf[vertexIndex].py = y.toFloat(); vbuf[vertexIndex].pz = z.toFloat()
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        val c = (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24)
        emitter.color(vertexIndex, c)
        vbuf[vertexIndex].color = c
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        emitter.uv(vertexIndex, u, v)
        vbuf[vertexIndex].u = u; vbuf[vertexIndex].v = v
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
        vbuf[vertexIndex].lightmap = v
        return this
    }

    override fun light(light: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, light)
        vbuf[vertexIndex].lightmap = light
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.normal(vertexIndex, x, y, z)
        vbuf[vertexIndex].nx = x; vbuf[vertexIndex].ny = y; vbuf[vertexIndex].nz = z
        return this
    }

    override fun next() {
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            if (currentDoubleSided) {
                for (i in 0..3) {
                    val d = vbuf[3 - i]
                    emitter.pos(i, d.px, d.py, d.pz)
                    emitter.color(i, d.color)
                    emitter.uv(i, d.u, d.v)
                    emitter.lightmap(i, d.lightmap)
                    emitter.normal(i, -d.nx, -d.ny, -d.nz)
                }
                emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
                emitter.emit()
            }
            vertexIndex = 0
        }
    }

    override fun fixedColor(red: Int, green: Int, blue: Int, alpha: Int) {
    }

    override fun unfixColor() {
    }

    override fun vertex(
        x: Float, y: Float, z: Float,
        red: Float, green: Float, blue: Float, alpha: Float,
        u: Float, v: Float,
        overlay: Int, light: Int,
        normalX: Float, normalY: Float, normalZ: Float
    ) {
        val c = ((alpha * 255).toInt() shl 24) or ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
        emitter.pos(vertexIndex, x, y, z)
        emitter.color(vertexIndex, c)
        emitter.uv(vertexIndex, u, v)
        emitter.lightmap(vertexIndex, light)
        emitter.normal(vertexIndex, normalX, normalY, normalZ)
        vbuf[vertexIndex].let { d ->
            d.px = x; d.py = y; d.pz = z; d.color = c
            d.u = u; d.v = v; d.lightmap = light
            d.nx = normalX; d.ny = normalY; d.nz = normalZ
        }
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            if (currentDoubleSided) {
                for (i in 0..3) {
                    val d = vbuf[3 - i]
                    emitter.pos(i, d.px, d.py, d.pz)
                    emitter.color(i, d.color)
                    emitter.uv(i, d.u, d.v)
                    emitter.lightmap(i, d.lightmap)
                    emitter.normal(i, -d.nx, -d.ny, -d.nz)
                }
                emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
                emitter.emit()
            }
            vertexIndex = 0
        }
    }

    fun build(): Mesh {
        return meshBuilder.build()
    }
}
*///?} elif <1.21.4 {
/*import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.Sprite

class MeshBuilderVertexConsumer(
    private val defaultSprite: Sprite,
    private val sprites: Map<String, Sprite>,
    private val doubleSidedMaterials: Set<String> = emptySet()
) : VertexConsumer {

    private val renderer: Renderer = RendererAccess.INSTANCE.renderer!!
    private val meshBuilder: MeshBuilder = renderer.meshBuilder()
    private val emitter = meshBuilder.emitter

    private var vertexIndex = 0
    private var material: String? = null
    private var currentDoubleSided = false

    private data class VertexData(
        var px: Float = 0f, var py: Float = 0f, var pz: Float = 0f,
        var color: Int = -1,
        var u: Float = 0f, var v: Float = 0f,
        var lightmap: Int = 0,
        var nx: Float = 0f, var ny: Float = 0f, var nz: Float = 0f
    )
    private val vbuf = Array(4) { VertexData() }

    fun material(material: String?) {
        this.material = material
        val key = material ?: "*"
        currentDoubleSided = key in doubleSidedMaterials || "*" in doubleSidedMaterials
    }

    override fun vertex(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.pos(vertexIndex, x, y, z)
        vbuf[vertexIndex].px = x; vbuf[vertexIndex].py = y; vbuf[vertexIndex].pz = z
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        val c = (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24)
        emitter.color(vertexIndex, c)
        vbuf[vertexIndex].color = c
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        emitter.uv(vertexIndex, u, v)
        vbuf[vertexIndex].u = u; vbuf[vertexIndex].v = v
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
        vbuf[vertexIndex].lightmap = v
        return this
    }

    override fun light(light: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, light)
        vbuf[vertexIndex].lightmap = light
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.normal(vertexIndex, x, y, z)
        vbuf[vertexIndex].nx = x; vbuf[vertexIndex].ny = y; vbuf[vertexIndex].nz = z
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
        vbuf[vertexIndex].let { d ->
            d.px = x; d.py = y; d.pz = z; d.color = color
            d.u = u; d.v = v; d.lightmap = light
            d.nx = normalX; d.ny = normalY; d.nz = normalZ
        }
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            if (currentDoubleSided) {
                for (i in 0..3) {
                    val d = vbuf[3 - i]
                    emitter.pos(i, d.px, d.py, d.pz)
                    emitter.color(i, d.color)
                    emitter.uv(i, d.u, d.v)
                    emitter.lightmap(i, d.lightmap)
                    emitter.normal(i, -d.nx, -d.ny, -d.nz)
                }
                emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
                emitter.emit()
            }
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

class MeshBuilderVertexConsumer(
    private val defaultSprite: Sprite,
    private val sprites: Map<String, Sprite>,
    private val doubleSidedMaterials: Set<String> = emptySet()
) : VertexConsumer {

    private val renderer: Renderer = Renderer.get()
    private val mutableMesh: MutableMesh = renderer.mutableMesh()
    private val emitter = mutableMesh.emitter()

    private var vertexIndex = 0
    private var material: String? = null
    private var currentDoubleSided = false

    private data class VertexData(
        var px: Float = 0f, var py: Float = 0f, var pz: Float = 0f,
        var color: Int = -1,
        var u: Float = 0f, var v: Float = 0f,
        var lightmap: Int = 0,
        var nx: Float = 0f, var ny: Float = 0f, var nz: Float = 0f
    )
    private val vbuf = Array(4) { VertexData() }

    fun material(material: String?) {
        this.material = material
        val key = material ?: "*"
        currentDoubleSided = key in doubleSidedMaterials || "*" in doubleSidedMaterials
    }

    override fun vertex(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.pos(vertexIndex, x, y, z)
        vbuf[vertexIndex].px = x; vbuf[vertexIndex].py = y; vbuf[vertexIndex].pz = z
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        val c = (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24)
        emitter.color(vertexIndex, c)
        vbuf[vertexIndex].color = c
        return this
    }

    //? if >=1.21.11 {
    override fun color(color: Int): VertexConsumer {
        emitter.color(vertexIndex, color)
        vbuf[vertexIndex].color = color
        return this
    }

    override fun lineWidth(width: Float): VertexConsumer {
        return this
    }
    //?}

    override fun texture(u: Float, v: Float): VertexConsumer {
        emitter.uv(vertexIndex, u, v)
        vbuf[vertexIndex].u = u; vbuf[vertexIndex].v = v
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
        vbuf[vertexIndex].lightmap = v
        return this
    }

    override fun light(light: Int): VertexConsumer {
        emitter.lightmap(vertexIndex, light)
        vbuf[vertexIndex].lightmap = light
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        emitter.normal(vertexIndex, x, y, z)
        vbuf[vertexIndex].nx = x; vbuf[vertexIndex].ny = y; vbuf[vertexIndex].nz = z
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
        vbuf[vertexIndex].let { d ->
            d.px = x; d.py = y; d.pz = z; d.color = color
            d.u = u; d.v = v; d.lightmap = light
            d.nx = normalX; d.ny = normalY; d.nz = normalZ
        }
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()
            if (currentDoubleSided) {
                for (i in 0..3) {
                    val d = vbuf[3 - i]
                    emitter.pos(i, d.px, d.py, d.pz)
                    emitter.color(i, d.color)
                    emitter.uv(i, d.u, d.v)
                    emitter.lightmap(i, d.lightmap)
                    emitter.normal(i, -d.nx, -d.ny, -d.nz)
                }
                emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
                emitter.emit()
            }
            vertexIndex = 0
        }
    }

    fun build(): Mesh {
        return mutableMesh.immutableCopy()
    }
}
//?}
