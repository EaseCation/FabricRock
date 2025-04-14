package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.Property
import java.util.*

data class BedrockIntProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<Int>
) : Property<Int>(bedrockName, Int::class.javaObjectType), BedrockProperty<Int, BedrockIntProperty> {
    companion object {
        fun of(bedrockName: String, values: Set<Int>): BedrockIntProperty {
            val name = bedrockName
            return BedrockIntProperty(bedrockName, name, values)
        }
    }

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Int): String = value.toString()

    override fun getValues(): Collection<Int> = values

    override fun parse(name: String): Optional<Int> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Int): String = value.toString()
}