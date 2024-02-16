package net.easecation.bedrockloader.bedrock.block.component

data class ComponentCollisionBox(
        val origin : FloatArray,
        val size : FloatArray
) : IBlockComponent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentCollisionBox

        if (!origin.contentEquals(other.origin)) return false
        if (!size.contentEquals(other.size)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.contentHashCode()
        result = 31 * result + size.contentHashCode()
        return result
    }
}
