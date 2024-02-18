package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class GeometryDefinition(
        val debug: Boolean?,
        val format_version: String,
        @SerializedName("minecraft:geometry") val geometry: List<Model>
) {

    data class Model(
            val description: Description,
            val bones: List<Bone>?,
            val cape: String?
    )

    data class Description(
            val identifier: String,
            val texture_width: Int?,
            val texture_height: Int?,
            val visible_bounds_offset: List<Float>?,
            val visible_bounds_width: Float?,
            val visible_bounds_height: Float?
    )

    data class Bone(
            val binding: String?,
            val cubes: List<Cube>?,
            val debug: Boolean?,
            val inflate: Float?,
            val locators: Map<String, Locator>?,
            val mirror: Boolean?,
            val name: String,
            val parent: String?,
            val pivot: List<Float>?,
            val poly_mesh: PolyMesh?,
            val render_group_id: Int?,
            val rotation: List<Float>?,
            val texture_meshes: List<TextureMeshes>?
    )

    data class Cube(
            val inflate: Float?,
            val mirror: Boolean?,
            val origin: List<Float>?,
            val pivot: List<Float>?,
            val reset: Boolean?,
            val rotation: List<Float>?,
            val size: List<Float>?,
            val uv: Uv?
    )

    sealed class Uv {
        data class UvBox(
                val uv: List<Int>?,
        ) : Uv()

        data class UvPerFace(
                val north: UvPerFaceData?,
                val south: UvPerFaceData?,
                val east: UvPerFaceData?,
                val west: UvPerFaceData?,
                val up: UvPerFaceData?,
                val down: UvPerFaceData?
        ) : Uv() {
            data class UvPerFaceData(
                    val uv: List<Int>?,
                    val uv_size: List<Int>?,
                    val material_instance: String?
            )
        }

        class Deserializer : JsonDeserializer<Uv> {
            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Uv {
                if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    return UvPerFace(
                            obj["north"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) },
                            obj["south"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) },
                            obj["east"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) },
                            obj["west"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) },
                            obj["up"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) },
                            obj["down"]?.let { context.deserialize(it, UvPerFace.UvPerFaceData::class.java) }
                    )
                } else {
                    return UvBox(json.asJsonArray.map { it.asInt })
                }
            }
        }
    }

    data class Locator(
            val offset: List<Int>?,
            val rotation: List<Int>?,
            val ignore_inherited_scale: Boolean?
    )

    data class PolyMesh(
            val normalized_uvs: Boolean?,
            val normals: List<List<Float>>?,
            val polys: List<List<Float>>?,
            val positions: List<List<Float>>?,
            val uvs: List<List<Int>>?
    )

    data class TextureMeshes(
            val local_pivot: List<Float>?,
            val position: List<Float>?,
            val rotation: List<Float>?,
            val scale: List<Float>?,
            val texture: String
    )

}