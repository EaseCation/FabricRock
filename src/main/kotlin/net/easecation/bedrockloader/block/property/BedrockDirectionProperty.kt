package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.DirectionProperty
import net.minecraft.util.math.Direction

class BedrockDirectionProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<Direction>
) : DirectionProperty(name, values), BedrockProperty<Direction, BedrockDirectionProperty> {
    companion object {
        fun of(bedrockName: String, values: Set<Direction> = enumValues<Direction>().toSet()): BedrockDirectionProperty {
            val name = bedrockName
            return BedrockDirectionProperty(bedrockName, name, values)
        }
    }

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Direction): String = value.asString()
}