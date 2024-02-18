/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import net.easecation.bedrockloader.render.VertexIndexedVertexConsumer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.*
import java.util.*
import java.util.stream.Stream

@Environment(value = EnvType.CLIENT)
class ModelPart(private val cuboids: List<Cuboid>, private val children: Map<String, ModelPart>) {
    var pivotX: Float = 0f
    var pivotY: Float = 0f
    var pivotZ: Float = 0f
    var pitch: Float = 0f
    var yaw: Float = 0f
    var roll: Float = 0f
    var visible: Boolean = true

    var transform: ModelTransform
        get() = ModelTransform.of(this.pivotX, this.pivotY, this.pivotZ, this.pitch, this.yaw, this.roll)
        set(rotationData) {
            this.pivotX = rotationData.pivotX
            this.pivotY = rotationData.pivotY
            this.pivotZ = rotationData.pivotZ
            this.pitch = rotationData.pitch
            this.yaw = rotationData.yaw
            this.roll = rotationData.roll
        }

    fun copyTransform(part: ModelPart) {
        this.pitch = part.pitch
        this.yaw = part.yaw
        this.roll = part.roll
        this.pivotX = part.pivotX
        this.pivotY = part.pivotY
        this.pivotZ = part.pivotZ
    }

    fun getChild(name: String): ModelPart {
        val modelPart = children[name] ?: throw NoSuchElementException("Can't find part $name")
        return modelPart
    }

    fun setPivot(x: Float, y: Float, z: Float) {
        this.pivotX = x
        this.pivotY = y
        this.pivotZ = z
    }

    fun setAngles(pitch: Float, yaw: Float, roll: Float) {
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
        matrices.translate((this.pivotX / 16.0f).toDouble(), (this.pivotY / 16.0f).toDouble(), (this.pivotZ / 16.0f).toDouble())
        if (this.roll != 0.0f) {
            matrices.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(this.roll))
        }
        if (this.yaw != 0.0f) {
            matrices.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(this.yaw))
        }
        if (this.pitch != 0.0f) {
            matrices.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(this.pitch))
        }
    }

    private fun renderCuboids(entry: MatrixStack.Entry, vertexConsumer: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        for (cuboid in this.cuboids) {
            cuboid.renderCuboid(entry, vertexConsumer, light, overlay, red, green, blue, alpha)
        }
    }

    private fun renderCuboids(entry: MatrixStack.Entry, vertexConsumer: VertexIndexedVertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
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
            val uv: Pair<Float, Float>,
            val uvSize: Pair<Float, Float>
    )

    @Environment(value = EnvType.CLIENT)
    class Cuboid(u: Int, v: Int, x: Float, y: Float, z: Float, sizeX: Float, sizeY: Float, sizeZ: Float, extraX: Float, extraY: Float, extraZ: Float, mirror: Boolean, textureWidth: Float, textureHeight: Float) {
        private val sides: Array<Quad?>
        val minX: Float
        val minY: Float
        val minZ: Float
        val maxX: Float
        val maxY: Float
        val maxZ: Float

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
            val vertexBottomNW = Vertex(adjustedMinX, adjustedMinY, adjustedMinZ, 0.0f, 0.0f)
            val vertexBottomNE = Vertex(adjustedMaxX, adjustedMinY, adjustedMinZ, 0.0f, 8.0f)
            val vertexTopNE = Vertex(adjustedMaxX, adjustedMaxY, adjustedMinZ, 8.0f, 8.0f)
            val vertexTopNW = Vertex(adjustedMinX, adjustedMaxY, adjustedMinZ, 8.0f, 0.0f)
            val vertexBottomSW = Vertex(adjustedMinX, adjustedMinY, adjustedMaxZ, 0.0f, 0.0f)
            val vertexBottomSE = Vertex(adjustedMaxX, adjustedMinY, adjustedMaxZ, 0.0f, 8.0f)
            val vertexTopSE = Vertex(adjustedMaxX, adjustedMaxY, adjustedMaxZ, 8.0f, 8.0f)
            val vertexTopSW = Vertex(adjustedMinX, adjustedMaxY, adjustedMaxZ, 8.0f, 0.0f)

            val textureOriginU = u.toFloat()
            val textureEndUZ = textureOriginU + sizeZ
            val textureEndUX = textureOriginU + sizeZ + sizeX
            val textureDoubleEndUX = textureOriginU + sizeZ + sizeX + sizeX
            val textureWrapU = textureOriginU + sizeZ + sizeX + sizeZ
            val textureFullWrapU = textureOriginU + sizeZ + sizeX + sizeZ + sizeX
            val textureOriginV = v.toFloat()
            val textureEndVZ = textureOriginV + sizeZ
            val textureEndVY = textureOriginV + sizeZ + sizeY

            sides[2] = Quad(arrayOf(vertexBottomSE, vertexBottomSW, vertexBottomNW, vertexBottomNE), textureEndUZ, textureOriginV, textureEndUX, textureEndVZ, textureWidth, textureHeight, mirror, Direction.DOWN)
            sides[3] = Quad(arrayOf(vertexTopNE, vertexTopNW, vertexTopSW, vertexTopSE), textureEndUX, textureEndVZ, textureDoubleEndUX, textureOriginV, textureWidth, textureHeight, mirror, Direction.UP)
            sides[1] = Quad(arrayOf(vertexBottomNW, vertexBottomSW, vertexTopSW, vertexTopNW), textureOriginU, textureEndVZ, textureEndUZ, textureEndVY, textureWidth, textureHeight, mirror, Direction.WEST)
            sides[4] = Quad(arrayOf(vertexBottomNE, vertexBottomNW, vertexTopNW, vertexTopNE), textureEndUZ, textureEndVZ, textureEndUX, textureEndVY, textureWidth, textureHeight, mirror, Direction.NORTH)
            sides[0] = Quad(arrayOf(vertexBottomSE, vertexBottomNE, vertexTopNE, vertexTopSE), textureEndUX, textureEndVZ, textureWrapU, textureEndVY, textureWidth, textureHeight, mirror, Direction.EAST)
            sides[5] = Quad(arrayOf(vertexBottomSW, vertexBottomSE, vertexTopSE, vertexTopSW), textureWrapU, textureEndVZ, textureFullWrapU, textureEndVY, textureWidth, textureHeight, mirror, Direction.SOUTH)
        }

        fun renderCuboid(entry: MatrixStack.Entry, vertexConsumer: VertexConsumer, light: Int, overlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
            val transformMatrix4f = entry.positionMatrix  // 变换矩阵
            val normalMatrix3f = entry.normalMatrix  // 法相矩阵
            for (quad in this.sides) {
                val directionUnitVector = quad!!.direction.copy()
                directionUnitVector.transform(normalMatrix3f)
                val directionUnitVectorX = directionUnitVector.x
                val directionUnitVectorY = directionUnitVector.y
                val directionUnitVectorZ = directionUnitVector.z
                val vertices = quad.vertices
                var vertexIndex = 0
                val verticesLength = vertices.size
                while (vertexIndex < verticesLength) {
                    val vertex = vertices[vertexIndex]
                    val vertexPosX = vertex.pos.x / 16.0f
                    val vertexPosY = vertex.pos.y / 16.0f
                    val vertexPosZ = vertex.pos.z / 16.0f
                    val quaternion = Vector4f(vertexPosX, vertexPosY, vertexPosZ, 1.0f)  // 普通坐标转为四元数的
                    quaternion.transform(transformMatrix4f)
                    if (vertexConsumer is VertexIndexedVertexConsumer) {
                        vertexConsumer.vertexIndex(vertexIndex)
                    }
                    vertexConsumer.vertex(
                            quaternion.x, quaternion.y, quaternion.z,
                            red, green, blue, alpha,
                            vertex.u, vertex.v,
                            overlay, light,
                            directionUnitVectorX, directionUnitVectorY, directionUnitVectorZ
                    )
                    vertexIndex++
                }
            }
        }
    }

    @Environment(value = EnvType.CLIENT)
    internal class Vertex(val pos: Vec3f, val u: Float, val v: Float) {
        constructor(x: Float, y: Float, z: Float, u: Float, v: Float) : this(Vec3f(x, y, z), u, v)

        fun remap(u: Float, v: Float): Vertex {
            return Vertex(this.pos, u, v)
        }
    }

    @Environment(value = EnvType.CLIENT)
    internal class Quad(val vertices: Array<Vertex>, u1: Float, v1: Float, u2: Float, v2: Float, squishU: Float, squishV: Float, flip: Boolean, direction: Direction) {
        val direction: Vec3f

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
                this.direction.multiplyComponentwise(-1.0f, 1.0f, 1.0f)
            }
        }
    }
}

