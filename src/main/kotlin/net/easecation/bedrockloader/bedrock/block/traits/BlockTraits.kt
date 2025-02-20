package net.easecation.bedrockloader.bedrock.block.traits

import com.google.gson.annotations.SerializedName

data class BlockTraits(
    @SerializedName("minecraft:placement_direction") val minecraftPlacementDirection: TraitPlacementDirection?,
    @SerializedName("minecraft:placement_position") val minecraftPlacementPosition: TraitPlacementPosition?,
)