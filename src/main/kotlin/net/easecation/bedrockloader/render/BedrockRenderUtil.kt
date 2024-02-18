package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.render.model.*
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.minecraft.client.util.math.MatrixStack

object BedrockRenderUtil {

    fun bedrockBonesToJavaModelData(bones: List<GeometryDefinition.Bone>) : ModelData {
        var boneCount = 0
        fun addBoneToModelData(bone: GeometryDefinition.Bone, parentPartData: ModelPartData) {
            val pivotTransform = ModelTransform.of(
                    bone.pivot?.get(0) ?: 0f,
                    bone.pivot?.get(1) ?: 0f,
                    bone.pivot?.get(2) ?: 0f,
                    bone.rotation?.get(0) ?: 0f,
                    bone.rotation?.get(1) ?: 0f,
                    bone.rotation?.get(2) ?: 0f
            )
            val bonePartData = parentPartData.addChild(bone.name, ModelPartBuilder.create().mirrored(bone.mirror == true), pivotTransform)

            bone.cubes?.forEach { cube ->
                val cubeBuilder = ModelPartBuilder.create().mirrored(cube.mirror == true)
                cube.uv?.let {
                    when (it) {
                        is GeometryDefinition.Uv.UvBox -> cubeBuilder.uv(it.uv?.get(0) ?: 0, it.uv?.get(1) ?: 0)
                        is GeometryDefinition.Uv.UvPerFace -> {}  // TODO 暂不支持逐面UV
                    }
                }
                cube.origin?.let { cubeBuilder
                        .cuboid(
                                it[0], it[1], it[2],
                                cube.size?.get(0) ?: 0f,
                                cube.size?.get(1) ?: 0f,
                                cube.size?.get(2) ?: 0f
                        )
                }
                bonePartData.addChild("cube${boneCount++}", cubeBuilder,
                        ModelTransform.of(
                                cube.pivot?.get(0) ?: 0f, cube.pivot?.get(1) ?: 0f, cube.pivot?.get(2) ?: 0f,
                                cube.rotation?.get(0) ?: 0f, cube.rotation?.get(1) ?: 0f, cube.rotation?.get(2) ?: 0f
                        )
                )
            }

            bones.filter { it.parent == bone.name }.forEach { childBone ->
                addBoneToModelData(childBone, bonePartData)
            }
        }

        val modelData = ModelData()
        val rootPartData = modelData.root
        for (bone in bones) {
            if (bone.parent == null) {
                addBoneToModelData(bone, rootPartData)
            }
        }
        return modelData
    }

    /**
     * Converts a ModelPart to a Mesh using a MeshBuilder.
     * @param modelPart The ModelPart to convert.
     * @return The created Mesh.
     */
    fun bakeModelPartToMesh(modelPart: ModelPart): Mesh {
        val matrixStack = MatrixStack()
        val vertices = MeshBuilderVertexConsumer()
        modelPart.render(matrixStack, vertices, 1, 1)
        return vertices.build()
    }

}