package net.easecation.bedrockloader.bedrock.block.state

data class StateRange(val values: RangeValues) : IBlockState {
    data class RangeValues(val min: Int, val max: Int)
}