package net.easecation.bedrockloader.java.definition

data class JavaBlockStatesDefinition(
        val variants: Map<String, Variant>
) {
    data class Variant(
            val model: String
    )
}
