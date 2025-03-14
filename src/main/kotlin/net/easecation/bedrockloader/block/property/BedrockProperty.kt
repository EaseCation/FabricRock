package net.easecation.bedrockloader.block.property

import net.minecraft.block.BlockState
import net.minecraft.state.property.Property

interface BedrockProperty<T, P> where T : Comparable<T>, P : Property<T>, P : BedrockProperty<T, P> {
    val javaProperty: P

    fun getBedrockName(): String

    fun getBedrockValueName(value: T): String

    fun getBedrockValueName(state: BlockState): String? {
        val value = state.getOrEmpty(javaProperty).orElse(null) ?: return null
        return getBedrockValueName(value)
    }
}