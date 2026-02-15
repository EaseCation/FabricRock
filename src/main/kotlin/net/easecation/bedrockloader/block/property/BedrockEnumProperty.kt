package net.easecation.bedrockloader.block.property

import net.minecraft.state.property.EnumProperty
//? if >=1.21.2 {
/*import net.minecraft.state.property.Property
*///?}
import net.minecraft.util.StringIdentifiable

class BedrockEnumProperty<T>(
    private val bedrockName: String,
    //? if >=1.21.2 {
    /*override val javaProperty: Property<T>
    *///?} else {
    private val name: String,
    private val type: Class<T>,
    private val values: Set<T>
    //?}
) //? if >=1.21.2 {
/*: BedrockProperty<T> where T : Enum<T>, T : StringIdentifiable {
*///?} else {
: EnumProperty<T>(name, type, values), BedrockProperty<T, BedrockEnumProperty<T>> where T : Enum<T>, T : StringIdentifiable {
//?}
    companion object {
        inline fun <reified T> of(bedrockName: String, values: Set<T> = enumValues<T>().toSet()): BedrockEnumProperty<T> where T : Enum<T>, T : StringIdentifiable {
            //? if >=1.21.2 {
            /*val javaProperty = EnumProperty.of(bedrockName, T::class.java, values.toList())
            return BedrockEnumProperty(bedrockName, javaProperty)
            *///?} else {
            val name = bedrockName
            return BedrockEnumProperty(bedrockName, name, T::class.java, values)
            //?}
        }
    }

    //? if <1.21.2 {
    override val javaProperty get() = this
    //?}

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: T): String = value.asString()
}