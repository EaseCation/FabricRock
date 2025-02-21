package net.easecation.bedrockloader.block

import net.minecraft.state.property.Property
import java.util.*

data class BedrockStringProperty(
    private val name: String,
    private val values: Set<String>
) : Property<String>(name, String::class.javaObjectType) {
    companion object {
        fun of(name: String, values: Set<String>): BedrockStringProperty {
            return BedrockStringProperty(name, values)
        }
    }

    override fun getValues(): Collection<String> = values

    override fun parse(name: String): Optional<String> = Optional.ofNullable(values.find { it == name })

    override fun name(value: String): String = value.toString()
}