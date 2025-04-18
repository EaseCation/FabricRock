package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.render.model.*
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.math.MatrixStack
import org.joml.Quaternionf

object BedrockRenderUtil {

    fun bedrockBonesToJavaModelData(bones: List<GeometryDefinition.Bone>) : ModelData {
        var boneCount = 0
        fun addBoneToModelData(bone: GeometryDefinition.Bone, parentPartData: ModelPartData) {
            val pivotTransform = ModelTransform.of(
                    (bone.pivot?.get(0) ?: 0.0),
                    -(bone.pivot?.get(1) ?: 0.0),
                    (bone.pivot?.get(2) ?: 0.0),
                    (bone.rotation?.get(0) ?: 0.0) * (Math.PI * 2 / 360.0),
                    (bone.rotation?.get(1) ?: 0.0) * (Math.PI * 2 / 360.0),
                    (bone.rotation?.get(2) ?: 0.0) * (Math.PI * 2 / 360.0),
                    bone.inflate ?: 0.0,
                    detachPivot = true
            )
            val bonePartData = parentPartData.addChild(bone.name, ModelPartBuilder.create(), pivotTransform)

            bone.cubes?.forEach { cube ->
                val cubeBuilder = ModelPartBuilder.create()
                cube.uv?.let {
                    when (it) {
                        is GeometryDefinition.Uv.UvBox -> cubeBuilder.uv(it.uv?.get(0) ?: 0, it.uv?.get(1) ?: 0)
                        is GeometryDefinition.Uv.UvPerFace -> {
                            cubeBuilder.uv(
                                ModelPart.FaceUV(
                                north = ModelPart.UVMapping(uv = Pair(it.north?.uv?.get(0) ?: 0, it.north?.uv?.get(1) ?: 0), uvSize = Pair(it.north?.uv_size?.get(0) ?: 0, it.north?.uv_size?.get(1) ?: 0), material = it.north?.material_instance),
                                east = ModelPart.UVMapping(uv = Pair(it.west?.uv?.get(0) ?: 0, it.west?.uv?.get(1) ?: 0), uvSize = Pair(it.west?.uv_size?.get(0) ?: 0, it.west?.uv_size?.get(1) ?: 0), material = it.west?.material_instance),
                                south = ModelPart.UVMapping(uv = Pair(it.south?.uv?.get(0) ?: 0, it.south?.uv?.get(1) ?: 0), uvSize = Pair(it.south?.uv_size?.get(0) ?: 0, it.south?.uv_size?.get(1) ?: 0), material = it.south?.material_instance),
                                west = ModelPart.UVMapping(uv = Pair(it.east?.uv?.get(0) ?: 0, it.east?.uv?.get(1) ?: 0), uvSize = Pair(it.east?.uv_size?.get(0) ?: 0, it.east?.uv_size?.get(1) ?: 0), material = it.east?.material_instance),
                                up = ModelPart.UVMapping(uv = Pair(it.down?.uv?.get(0) ?: 0, it.down?.uv?.get(1) ?: 0), uvSize = Pair(it.down?.uv_size?.get(0) ?: 0, it.down?.uv_size?.get(1) ?: 0), material = it.down?.material_instance),
                                down = ModelPart.UVMapping(uv = Pair(it.up?.uv?.get(0) ?: 0, it.up?.uv?.get(1) ?: 0), uvSize = Pair(it.up?.uv_size?.get(0) ?: 0, it.up?.uv_size?.get(1) ?: 0), material = it.up?.material_instance),
                            ))
                        }
                    }
                }
                cube.origin?.let {
                        val size = cube.size ?: listOf(0.0, 0.0, 0.0)
                        val offset = listOf(
                            it[0] + size[0] / 2,
                            -it[1] - size[1],
                            it[2] + size[2] / 2
                        )
                        cubeBuilder.cuboid(
                                offset[0],
                                offset[1],
                                offset[2],
                                size[0],
                                size[1],
                                size[2]
                        )
                }
                val rotationData = ModelTransform.of(
                    (cube.pivot?.get(0) ?: 0.0),
                    -(cube.pivot?.get(1) ?: 0.0),
                    (cube.pivot?.get(2) ?: 0.0),
                    (cube.rotation?.get(0) ?: 0.0) * (Math.PI * 2 / 360.0),
                    (cube.rotation?.get(1) ?: 0.0) * (Math.PI * 2 / 360.0),
                    (cube.rotation?.get(2) ?: 0.0) * (Math.PI * 2 / 360.0),
                    cube.inflate ?: 0.0,
                    detachPivot = true
                )
                bonePartData.addChild("cube${boneCount++}", cubeBuilder, rotationData)
            }

            bones.filter { it.parent == bone.name }.forEach { childBone ->
                addBoneToModelData(childBone, bonePartData)
            }
        }

        val modelData = ModelData()
        val root = modelData.root
        for (bone in bones) {
            if (bone.parent == null) {
                addBoneToModelData(bone, root)
            }
        }
        return modelData
    }

    /**
     * Converts a ModelPart to a Mesh using a MeshBuilder.
     * @param modelPart The ModelPart to convert.
     * @return The created Mesh.
     */
    fun bakeModelPartToMesh(
        modelPart: ModelPart,
        defaultSprite: Sprite,
        sprites: Map<String, Sprite>,
        blockTransformation: ComponentTransformation?
    ): Mesh {
        val matrixStack = MatrixStack()
        val entry = matrixStack.peek()
        blockTransformation?.apply(entry.positionMatrix, entry.normalMatrix)
        matrixStack.translate(0.5, 0.0, 0.5)
        matrixStack.multiply(Quaternionf().rotateXYZ((180.0 * (Math.PI * 2 / 360.0)).toFloat(), (180.0 * (Math.PI * 2 / 360.0)).toFloat(), 0.0F))
        val vertices = MeshBuilderVertexConsumer(defaultSprite, sprites)
        modelPart.render(matrixStack, vertices, 1, 1)
        return vertices.build()
    }

}