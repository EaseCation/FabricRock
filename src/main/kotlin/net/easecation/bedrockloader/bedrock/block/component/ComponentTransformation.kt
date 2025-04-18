package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import org.joml.*
import java.lang.Math
import java.lang.reflect.Type

/**
 * scale -> rotation -> translation
 */
data class ComponentTransformation(
    val rotation: FloatArray,
    val rotation_pivot: FloatArray,
    val scale: FloatArray,
    val scale_pivot: FloatArray,
    val translation: FloatArray,
) : IBlockComponent {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentTransformation

        if (!rotation.contentEquals(other.rotation)) return false
        if (!rotation_pivot.contentEquals(other.rotation_pivot)) return false
        if (!scale.contentEquals(other.scale)) return false
        if (!scale_pivot.contentEquals(other.scale_pivot)) return false
        if (!translation.contentEquals(other.translation)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rotation.contentHashCode()
        result = 31 * result + rotation_pivot.contentHashCode()
        result = 31 * result + scale.contentHashCode()
        result = 31 * result + scale_pivot.contentHashCode()
        result = 31 * result + translation.contentHashCode()
        return result
    }

    fun apply(box: Box): Box {
        val matrix = createMatrix()
        val p1 = Vector3d(box.minX, box.minY, box.minZ).mulProject(matrix)
        val p2 = Vector3d(box.maxX, box.maxY, box.maxZ).mulProject(matrix)
        return Box(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z)
    }

    private fun createMatrix(): Matrix4d {
        val rotation = Vector3d(Math.toRadians(rotation[0].toDouble()), Math.toRadians(rotation[1].toDouble()), Math.toRadians(rotation[2].toDouble()))
        val rotationPivot = Vector3d(rotation_pivot[0].toDouble(), rotation_pivot[1].toDouble(), rotation_pivot[2].toDouble())
        val scale = Vector3d(scale[0].toDouble(), scale[1].toDouble(), scale[2].toDouble())
        val scalePivot = Vector3d(scale_pivot[0].toDouble(), scale_pivot[1].toDouble(), scale_pivot[2].toDouble())
        val translation = Vector3d(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble())
        return createMatrix(rotation, rotationPivot, scale, scalePivot, translation)
    }

    private fun createMatrix(
        rotation: Vector3d,
        rotationPivot: Vector3d,
        scale: Vector3d,
        scalePivot: Vector3d,
        translation: Vector3d
    ): Matrix4d = Matrix4d()
        .scaleAround(scale.x, scale.y, scale.z, scalePivot.x + 0.5, scalePivot.y + 0.5, scalePivot.z + 0.5)
        .rotateAround(Quaterniond().rotateZYX(rotation.z, rotation.y, rotation.x), rotationPivot.x + 0.5, rotationPivot.y + 0.5, rotationPivot.z + 0.5)
        .translate(translation)

    fun apply(position: Matrix4f, normal: Matrix3f) {
        val rotation = Vector3f(Math.toRadians(rotation[0].toDouble()).toFloat(), Math.toRadians(rotation[1].toDouble()).toFloat(), Math.toRadians(rotation[2].toDouble()).toFloat())
        val rotationPivot = Vector3f(rotation_pivot[0], rotation_pivot[1], rotation_pivot[2])
        val scale = Vector3f(scale[0], scale[1], scale[2])
        val scalePivot = Vector3f(scale_pivot[0], scale_pivot[1], scale_pivot[2])
        val translation = Vector3f(translation[0], translation[1], translation[2])

        position.scaleAround(scale.x, scale.y, scale.z, scalePivot.x + 0.5f, scalePivot.y + 0.5f, scalePivot.z + 0.5f)
        position.rotateAround(Quaternionf().rotateZYX(rotation.z, rotation.y, rotation.x), rotationPivot.x + 0.5f, rotationPivot.y + 0.5f, rotationPivot.z + 0.5f)
        position.translate(translation)
        val invScaleX: Float = 1.0f / scale.x
        val invScaleY: Float = 1.0f / scale.y
        val invScaleZ: Float = 1.0f / scale.z
        val invCbrt = MathHelper.fastInverseCbrt(invScaleX * invScaleY * invScaleZ)
        normal.scale(invScaleX * invCbrt, invScaleY * invCbrt, invScaleZ * invCbrt)
        normal.rotate(Quaternionf().rotateZYX(rotation.z, rotation.y, rotation.x))
    }

    class Deserializer : JsonDeserializer<ComponentTransformation> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentTransformation {
            val obj = json.asJsonObject
            val rotation = obj["rotation"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
            val rotation_pivot = obj["rotation_pivot"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
            val scale = obj["scale"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(1f, 1f, 1f)
            val scale_pivot = obj["scale_pivot"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
            val translation = obj["translation"]?.asJsonArray?.map { it.asFloat }?.toFloatArray() ?: floatArrayOf(0f, 0f, 0f)
            return ComponentTransformation(rotation, rotation_pivot, scale, scale_pivot, translation)
        }
    }
}