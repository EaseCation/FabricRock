package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.Property
import java.util.*

data class BedrockStringProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<String>
//? if >=1.21.2 {
/*) : Property<String>(bedrockName, String::class.javaObjectType), BedrockProperty<String> {
*///?} else {
) : Property<String>(bedrockName, String::class.javaObjectType), BedrockProperty<String, BedrockStringProperty> {
//?}
    companion object {
        fun of(bedrockName: String, values: Set<String>): BedrockStringProperty {
            val name = bedrockName
            return BedrockStringProperty(bedrockName, name, values)
        }
    }

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: String): String = value.toString()

    //? if >=1.21.2 {
    /*override fun ordinal(value: String): Int {
        return values.toList().indexOf(value)
    }

    override fun getValues(): List<String> {
        return values.toList()
    }
    *///?} else {
    override fun getValues(): Collection<String> = values
    //?}

    override fun parse(name: String): Optional<String> = Optional.ofNullable(values.find { it == name })

    override fun name(value: String): String = value
}