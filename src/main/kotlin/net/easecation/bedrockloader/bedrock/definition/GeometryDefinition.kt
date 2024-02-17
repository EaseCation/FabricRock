package net.easecation.bedrockloader.bedrock.definition

data class GeometryDefinition(
        val debug: Boolean?,
        val format_version: String,
        val minecraft: Map<String, List<Model>>
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
            val visible_bounds_offset: List<Int>?,
            val visible_bounds_width: Int?,
            val visible_bounds_height: Int?
    )

    data class Bone(
            val binding: String?,
            val cubes: List<Cube>?,
            val debug: Boolean?,
            val inflate: Int?,
            val locators: Map<String, Locator>?,
            val mirror: Boolean?,
            val name: String,
            val parent: String?,
            val pivot: List<Int>?,
            val poly_mesh: PolyMesh?,
            val render_group_id: Int?,
            val rotation: List<Int>?,
            val texture_meshes: List<TextureMeshes>?
    )

    data class Cube(
            val inflate: Int?,
            val mirror: Boolean?,
            val origin: List<Int>?,
            val pivot: List<Int>?,
            val reset: Boolean?,
            val rotation: List<Int>?,
            val size: List<Int>?,
            val uv: Uv?
    )

    data class Uv(
            val north: North?,
            val south: South?,
            val east: East?,
            val west: West?,
            val up: Up?,
            val down: Down?
    )

    data class North(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class South(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class East(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class West(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class Up(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class Down(
            val uv: List<Int>?,
            val uv_size: List<Int>?,
            val material_instance: String?
    )

    data class Locator(
            val offset: List<Int>?,
            val rotation: List<Int>?,
            val ignore_inherited_scale: Boolean?
    )

    data class PolyMesh(
            val normalized_uvs: Boolean?,
            val normals: List<List<Int>>?,
            val polys: List<List<Int>>?,
            val positions: List<List<Int>>?,
            val uvs: List<List<Int>>?
    )

    data class TextureMeshes(
            val local_pivot: List<Int>?,
            val position: List<Int>?,
            val rotation: List<Int>?,
            val scale: List<Int>?,
            val texture: String
    )

}