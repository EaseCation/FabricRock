package net.easecation.bedrockloader.bedrock.block.component

data class ComponentBlockEntity(
    val tick: Boolean?,
    val client_tick: Boolean?,
    val movable: Boolean?
) : IBlockComponent
