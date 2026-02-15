package net.easecation.bedrockloader.java.definition

data class JavaMCMeta(
        val pack: PackInfo
) {

    data class PackInfo(
            val pack_format: Int,
            val description: String,
            val min_format: Int? = null,
            val max_format: Int? = null
    )
}
