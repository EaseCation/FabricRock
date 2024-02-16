package net.easecation.bedrockloader.java.definition

data class JavaMCMeta(
        val pack: PackInfo
) {

    data class PackInfo(
            val pack_format: Int,
            val description: String
    )
}
