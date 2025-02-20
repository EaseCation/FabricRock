package net.easecation.bedrockloader.block

import net.minecraft.state.property.Property
import java.util.*

data class BedrockIntProperty(
    private val name: String,
    private val values: Set<Int>
) : Property<Int>(name, Int::class.java) {
    companion object {
        fun of(name: String, values: Set<Int>): BedrockIntProperty {
            return BedrockIntProperty(name, values)
        }
    }

    override fun getValues(): Collection<Int> = values

    override fun parse(name: String): Optional<Int> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Int): String = value.toString()
}