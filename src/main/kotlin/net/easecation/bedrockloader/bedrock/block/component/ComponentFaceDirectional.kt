package net.easecation.bedrockloader.bedrock.block.component

data class ComponentFaceDirectional(
    val type: FaceDirectionalType,
) : IBlockComponent

enum class FaceDirectionalType {
    direction,
    facing_direction
}