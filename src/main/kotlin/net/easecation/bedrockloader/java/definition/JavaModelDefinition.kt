package net.easecation.bedrockloader.java.definition

import net.minecraft.util.Identifier

data class JavaModelDefinition(
        var parent: String? = null,
        var textures: Map<String, Identifier>? = null
)
