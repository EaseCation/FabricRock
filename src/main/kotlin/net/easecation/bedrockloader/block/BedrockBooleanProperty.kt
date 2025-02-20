package net.easecation.bedrockloader.block

import net.minecraft.state.property.Property
import java.util.*

data class BedrockBooleanProperty(
    private val name: String,
    private val values: Set<Boolean>
) : Property<Boolean>(name, Boolean::class.java) {
    companion object {
        fun of(name: String, values: Set<Boolean>): BedrockBooleanProperty {
            return BedrockBooleanProperty(name, values)
        }
    }

    override fun getValues(): Collection<Boolean> = values

    override fun parse(name: String): Optional<Boolean> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Boolean): String = value.toString()
}