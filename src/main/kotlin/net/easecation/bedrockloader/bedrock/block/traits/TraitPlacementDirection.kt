package net.easecation.bedrockloader.bedrock.block.traits

import com.google.gson.annotations.SerializedName

data class TraitPlacementDirection(
    val enabled_states: Set<State>,
    val y_rotation_offset: Int
) : IBlockTrait {
    enum class State {
        @SerializedName("minecraft:cardinal_direction")
        MINECRAFT_CARDINAL_DIRECTION,
        @SerializedName("minecraft:facing_direction")
        MINECRAFT_FACING_DIRECTION
    }
}