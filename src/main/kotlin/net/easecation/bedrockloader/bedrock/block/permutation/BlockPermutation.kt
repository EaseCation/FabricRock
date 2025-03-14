package net.easecation.bedrockloader.bedrock.block.permutation

import net.easecation.bedrockloader.bedrock.block.component.BlockComponents

data class BlockPermutation(
    val condition: String,
    val components: BlockComponents
)