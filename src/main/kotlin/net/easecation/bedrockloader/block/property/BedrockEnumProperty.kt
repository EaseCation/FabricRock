package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.EnumProperty
import net.minecraft.util.StringIdentifiable

class BedrockEnumProperty<T>(
    private val bedrockName: String,
    private val name: String,
    private val type: Class<T>,
    private val values: Set<T>
) : EnumProperty<T>(name, type, values), BedrockProperty<T, BedrockEnumProperty<T>> where T : Enum<T>, T : StringIdentifiable {
    companion object {
        inline fun <reified T> of(bedrockName: String, values: Set<T> = enumValues<T>().toSet()): BedrockEnumProperty<T> where T : Enum<T>, T : StringIdentifiable {
            val name = bedrockName
            return BedrockEnumProperty(bedrockName, name, T::class.java, values)
        }
    }

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: T): String = value.asString()
}