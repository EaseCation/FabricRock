package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.Property
import net.minecraft.util.StringIdentifiable
import java.util.*

data class BedrockBooleanProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<Boolean>
) : Property<Boolean>(bedrockName, Boolean::class.javaObjectType), BedrockProperty<Boolean, BedrockBooleanProperty> {
    companion object {
        fun of(bedrockName: String, values: Set<Boolean>): BedrockBooleanProperty {
            val name = bedrockName.replace(':', '_').lowercase()
            return BedrockBooleanProperty(bedrockName, name, values)
        }
    }

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Boolean): String = value.toString()

    override fun getValues(): Collection<Boolean> = values

    override fun parse(name: String): Optional<Boolean> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Boolean): String = value.toString()
}