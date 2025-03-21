/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.easecation.bedrockloader.render.MeshBuilderVertexConsumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Direction
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import java.util.stream.Stream

@Environment(value = EnvType.CLIENT)
class ModelPart(private val cuboids: List<Cuboid>, private val children: Map<String, ModelPart>) {
    var pivotX: Double = 0.0
    var pivotY: Double = 0.0
    var pivotZ: Double = 0.0
    var pitch: Double = 0.0
    var yaw: Double = 0.0
    var roll: Double = 0.0
    var inflate: Double = 0.0
    var detachPivot: Boolean = false
    var visible: Boolean = true

    var transform: ModelTransform
        get() = ModelTransform.of(
            this.pivotX,
            this.pivotY,
            this.pivotZ,
            this.pitch,
            this.yaw,
            this.roll,
            this.inflate,
            this.detachPivot
        )
        set(rotationData) {
            this.pivotX = rotationData.pivotX
            this.pivotY = rotationData.pivotY
            this.pivotZ = rotationData.pivotZ
            this.pitch = rotationData.pitch
            this.yaw = rotationData.yaw
            this.roll = rotationData.roll
            this.inflate = rotationData.inflate
            this.detachPivot = rotationData.detachPivot
        }

    fun copyTransform(part: ModelPart) {
        this.pitch = part.pitch
        this.yaw = part.yaw
        this.roll = part.roll
        this.pivotX = part.pivotX
        this.pivotY = part.pivotY
        this.pivotZ = part.pivotZ
        this.inflate = part.inflate
        this.detachPivot = part.detachPivot
    }

    fun getChild(name: String): ModelPart {
        val modelPart = children[name] ?: throw NoSuchElementException("Can't find part $name")
        return modelPart
    }

    fun setPivot(x: Double, y: Double, z: Double) {
        this.pivotX = x
        this.pivotY = y
        this.pivotZ = z
    }

    fun setAngles(pitch: Double, yaw: Double, roll: Double) {
        this.pitch = pitch
        this.yaw = yaw
        this.roll = roll
    }

    @JvmOverloads
    fun render(matrices: MatrixStack, vertices: VertexConsumer, light: Int, overlay: Int, red: Float = 1.0f, green: Float = 1.0f, blue: Float = 1.0f, alpha: Float = 1.0f) {
        if (!this.visible) {
            return
        }
        if (cuboids.isEmpty() && children.isEmpty()) {
            return
        }
        matrices.push()
        val scale = (1.0f + (this.inflate / 16.0f)).toFloat()
        matrices.scale(scale, scale, scale)
        this.rotate(matrices)
        this.renderCuboids(matrices.peek(), vertices, light, overlay, red, green, blue, alpha)
        for (modelPart in children.values) {
            modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha)
        }
        matrices.pop()
    }

    fun forEachCuboid(matrices: MatrixStack, consumer: CuboidConsumer) {
        this.forEachCuboid(matrices, consumer, "")
    }

    private fun forEachCuboid(matrices: MatrixStack, consumer: CuboidConsumer, path: String) {
        if (cuboids.isEmpty() && children.isEmpty()) {
            return
        }
        matrices.push()
        val scale = (1.0f + (this.inflate / 16.0f)).toFloat()
        matrices.scale(scale, scale, scale)
        this.rotate(matrices)
        val entry = matrices.peek()
        for (i in cuboids.indices) {
            consumer.accept(entry, path, i, cuboids[i])
        }
        val string = "$path/"
        children.forEach { (name: String, part: ModelPart) -> part.forEachCuboid(matrices, consumer, string + name) }
        matrices.pop()
    }

    fun rotate(matrices: MatrixStack) {
        if (detachPivot) {
            matrices.multiply(
                Quaternionf().rotationZYX(this.roll.toFloat(), this.yaw.toFloat(), this.pitch.toFloat()),
                (this.pivotX / 16.0f).toFloat(),
                (this.pivotY / 16.0f).toFloat(),
                (this.pivotZ / 16.0f).toFloat()
            )
        } else {
            matrices.translate(this.pivotX / 16.0f, this.pivotY / 16.0f, this.pivotZ / 16.0f)
            if (this.pitch != 0.0 || (this.yaw != 0.0) || (this.roll != 0.0)) {
                matrices.multiply(Quaternionf().rotationZYX(this.roll.toFloat(), this.yaw.toFloat(), this.pitch.toFloat()))
            }
        }
    }

    private fun renderCuboids(entry: MatrixStack.Entry, vertexConsumer: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        for (cuboid in this.cuboids) {
            cuboid.renderCuboid(entry, vertexConsumer, light, overlay, red, green, blue, alpha)
        }
    }

    fun getRandomCuboid(random: Random): Cuboid {
        return cuboids[random.nextInt(cuboids.size)]
    }

    val isEmpty: Boolean
        get() = cuboids.isEmpty()

    fun traverse(): Stream<ModelPart> {
        return Stream.concat(Stream.of(this), children.values.stream().flatMap { obj: ModelPart -> obj.traverse() })
    }

    @Environment(value = EnvType.CLIENT)
    fun interface CuboidConsumer {
        fun accept(var1: MatrixStack.Entry?, var2: String?, var3: Int, var4: Cuboid?)
    }

    @Environment(value = EnvType.CLIENT)
    data class FaceUV(
        val north: UVMapping,
        val east: UVMapping,
        val south: UVMapping,
        val west: UVMapping,
        val up: UVMapping,
        val down: UVMapping
    )

    @Environment(value = EnvType.CLIENT)
    data class UVMapping(
            val uv: Pair<Int, Int>,
            val uvSize: Pair<Int, Int>,
            val material: String?
    )

    @Environment(value = EnvType.CLIENT)
    class Cuboid(u: Int, v: Int, faceUV: FaceUV?, x: Double, y: Double, z: Double, sizeX: Double, sizeY: Double, sizeZ: Double, extraX: Double, extraY: Double, extraZ: Double, mirror: Boolean, textureWidth: Double, textureHeight: Double) {
        private val sides: Array<Quad?>
        val minX: Double
        val minY: Double
        val minZ: Double
        val maxX: Double
        val maxY: Double
        val maxZ: Double

        init {
            this.minX = x - sizeX / 2
            this.minY = y
            this.minZ = z - sizeZ / 2
            this.maxX = x + sizeX / 2
            this.maxY = y + sizeY
            this.maxZ = z + sizeZ / 2
            this.sides = arrayOfNulls(6)
            var adjustedMinX = this.minX - extraX
            var adjustedMinY = this.minY - extraY
            var adjustedMinZ = this.minZ - extraZ
            var adjustedMaxX = this.maxX + extraX
            var adjustedMaxY = this.maxY + extraY
            var adjustedMaxZ = this.maxZ + extraZ
            if (mirror) {
                val tempX = adjustedMaxX
                adjustedMaxX = adjustedMinX
                adjustedMinX = tempX
            }
            val vertexBottomNW = Vertex(adjustedMinX, adjustedMinY, adjustedMinZ, 0.0, 0.0)
            val vertexBottomNE = Vertex(adjustedMaxX, adjustedMinY, adjustedMinZ, 0.0, 8.0)
            val vertexTopNE = Vertex(adjustedMaxX, adjustedMaxY, adjustedMinZ, 8.0, 8.0)
            val vertexTopNW = Vertex(adjustedMinX, adjustedMaxY, adjustedMinZ, 8.0, 0.0)
            val vertexBottomSW = Vertex(adjustedMinX, adjustedMinY, adjustedMaxZ, 0.0, 0.0)
            val vertexBottomSE = Vertex(adjustedMaxX, adjustedMinY, adjustedMaxZ, 0.0, 8.0)
            val vertexTopSE = Vertex(adjustedMaxX, adjustedMaxY, adjustedMaxZ, 8.0, 8.0)
            val vertexTopSW = Vertex(adjustedMinX, adjustedMaxY, adjustedMaxZ, 8.0, 0.0)

            val textureOriginU = u.toDouble()
            val textureEndUZ = textureOriginU + sizeZ
            val textureEndUX = textureOriginU + sizeZ + sizeX
            val textureDoubleEndUX = textureOriginU + sizeZ + sizeX + sizeX
            val textureWrapU = textureOriginU + sizeZ + sizeX + sizeZ
            val textureFullWrapU = textureOriginU + sizeZ + sizeX + sizeZ + sizeX
            val textureOriginV = v.toDouble()
            val textureEndVZ = textureOriginV + sizeZ
            val textureEndVY = textureOriginV + sizeZ + sizeY

            if (faceUV == null) {
                sides[2] = Quad(arrayOf(vertexBottomSE, vertexBottomSW, vertexBottomNW, vertexBottomNE), null, textureEndUZ, textureOriginV, textureEndUX, textureEndVZ, textureWidth, textureHeight, mirror, Direction.DOWN)
                sides[3] = Quad(arrayOf(vertexTopNE, vertexTopNW, vertexTopSW, vertexTopSE), null, textureEndUX, textureEndVZ, textureDoubleEndUX, textureOriginV, textureWidth, textureHeight, mirror, Direction.UP)
                sides[1] = Quad(arrayOf(vertexBottomNW, vertexBottomSW, vertexTopSW, vertexTopNW), null, textureOriginU, textureEndVZ, textureEndUZ, textureEndVY, textureWidth, textureHeight, mirror, Direction.WEST)
                sides[4] = Quad(arrayOf(vertexBottomNE, vertexBottomNW, vertexTopNW, vertexTopNE), null, textureEndUZ, textureEndVZ, textureEndUX, textureEndVY, textureWidth, textureHeight, mirror, Direction.NORTH)
                sides[0] = Quad(arrayOf(vertexBottomSE, vertexBottomNE, vertexTopNE, vertexTopSE), null, textureEndUX, textureEndVZ, textureWrapU, textureEndVY, textureWidth, textureHeight, mirror, Direction.EAST)
                sides[5] = Quad(arrayOf(vertexBottomSW, vertexBottomSE, vertexTopSE, vertexTopSW), null, textureWrapU, textureEndVZ, textureFullWrapU, textureEndVY, textureWidth, textureHeight, mirror, Direction.SOUTH)
            } else {
                sides[2] = Quad(arrayOf(vertexBottomSE, vertexBottomSW, vertexBottomNW, vertexBottomNE), faceUV.down.material, faceUV.down.uv.first.toDouble(), faceUV.down.uv.second.toDouble(), faceUV.down.uv.first.toDouble() + faceUV.down.uvSize.first.toDouble(), faceUV.down.uv.second.toDouble() + faceUV.down.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.DOWN)
                sides[3] = Quad(arrayOf(vertexTopNE, vertexTopNW, vertexTopSW, vertexTopSE), faceUV.up.material, faceUV.up.uv.first.toDouble(), faceUV.up.uv.second.toDouble(), faceUV.up.uv.first.toDouble() + faceUV.up.uvSize.first.toDouble(), faceUV.up.uv.second.toDouble() + faceUV.up.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.UP)
                sides[1] = Quad(arrayOf(vertexBottomNW, vertexBottomSW, vertexTopSW, vertexTopNW), faceUV.west.material, faceUV.west.uv.first.toDouble(), faceUV.west.uv.second.toDouble(), faceUV.west.uv.first.toDouble() + faceUV.west.uvSize.first.toDouble(), faceUV.west.uv.second.toDouble() + faceUV.west.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.WEST)
                sides[4] = Quad(arrayOf(vertexBottomNE, vertexBottomNW, vertexTopNW, vertexTopNE), faceUV.north.material, faceUV.north.uv.first.toDouble(), faceUV.north.uv.second.toDouble(), faceUV.north.uv.first.toDouble() + faceUV.north.uvSize.first.toDouble(), faceUV.north.uv.second.toDouble() + faceUV.north.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.NORTH)
                sides[0] = Quad(arrayOf(vertexBottomSE, vertexBottomNE, vertexTopNE, vertexTopSE), faceUV.east.material, faceUV.east.uv.first.toDouble(), faceUV.east.uv.second.toDouble(), faceUV.east.uv.first.toDouble() + faceUV.east.uvSize.first.toDouble(), faceUV.east.uv.second.toDouble() + faceUV.east.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.EAST)
                sides[5] = Quad(arrayOf(vertexBottomSW, vertexBottomSE, vertexTopSE, vertexTopSW), faceUV.south.material, faceUV.south.uv.first.toDouble(), faceUV.south.uv.second.toDouble(), faceUV.south.uv.first.toDouble() + faceUV.south.uvSize.first.toDouble(), faceUV.south.uv.second.toDouble() + faceUV.south.uvSize.second.toDouble(), textureWidth, textureHeight, mirror, Direction.SOUTH)
            }
        }

        fun renderCuboid(
            entry: MatrixStack.Entry,
            vertexConsumer: VertexConsumer,
            light: Int,
            overlay: Int,
            red: Float,
            green: Float,
            blue: Float,
            alpha: Float
        ) {
            val matrix4f = entry.positionMatrix
            val vector3f = Vector3f()

            for (quad in this.sides) {
                val vector3f2 = entry.normalMatrix.transform(quad!!.direction, vector3f)
                val f = vector3f2.x()
                val g = vector3f2.y()
                val h = vector3f2.z()

                if (vertexConsumer is MeshBuilderVertexConsumer) vertexConsumer.material(quad.material)

                for (vertex in quad.vertices) {
                    val i = vertex.pos.x() / 16.0f
                    val j = vertex.pos.y() / 16.0f
                    val k = vertex.pos.z() / 16.0f
                    val vector3f3 = matrix4f.transformPosition(i.toFloat(), j.toFloat(), k.toFloat(), vector3f)
                    vertexConsumer.vertex(vector3f3.x(), vector3f3.y(), vector3f3.z(), red, green, blue, alpha, vertex.u.toFloat(), vertex.v.toFloat(), overlay, light, f, g, h)
                }
            }
        }
    }

    @Environment(value = EnvType.CLIENT)
    internal class Vertex(val pos: Vector3d, val u: Double, val v: Double) {
        constructor(x: Double, y: Double, z: Double, u: Double, v: Double) : this(Vector3d(x, y, z), u, v)

        fun remap(u: Double, v: Double): Vertex {
            return Vertex(this.pos, u, v)
        }
    }

    @Environment(value = EnvType.CLIENT)
    internal class Quad(val vertices: Array<Vertex>, val material: String?, u1: Double, v1: Double, u2: Double, v2: Double, squishU: Double, squishV: Double, flip: Boolean, direction: Direction) {
        val direction: Vector3f

        init {
            val f = 0.0f / squishU
            val g = 0.0f / squishV
            vertices[0] = vertices[0].remap(u2 / squishU - f, v1 / squishV + g)
            vertices[1] = vertices[1].remap(u1 / squishU + f, v1 / squishV + g)
            vertices[2] = vertices[2].remap(u1 / squishU + f, v2 / squishV - g)
            vertices[3] = vertices[3].remap(u2 / squishU - f, v2 / squishV - g)
            if (flip) {
                val i = vertices.size
                for (j in 0 until i / 2) {
                    val vertex = vertices[j]
                    vertices[j] = vertices[i - 1 - j]
                    vertices[i - 1 - j] = vertex
                }
            }
            this.direction = direction.unitVector
            if (flip) {
                this.direction.mul(-1.0f, 1.0f, 1.0f)
            }
        }
    }
}

