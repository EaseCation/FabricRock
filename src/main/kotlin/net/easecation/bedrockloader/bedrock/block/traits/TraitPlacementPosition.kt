package net.easecation.bedrockloader.bedrock.block.traits

import com.google.gson.annotations.SerializedName

data class TraitPlacementPosition(
    val enabled_states: Set<State>
) : IBlockTrait {
    enum class State {
        @SerializedName("minecraft:block_face")
        MINECRAFT_BLOCK_FACE,
        @SerializedName("minecraft:vertical_half")
        MINECRAFT_VERTICAL_HALF
    }
}
