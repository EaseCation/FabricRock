package net.easecation.bedrockloader.block.property

//? if >=1.21.2 {
/*import net.minecraft.state.property.Properties
import net.minecraft.state.property.Property
*///?} else {
import net.minecraft.state.property.DirectionProperty
//?}
import net.minecraft.util.math.Direction

class BedrockDirectionProperty(
    private val bedrockName: String,
    //? if >=1.21.2 {
    /*override val javaProperty: Property<Direction>
    *///?} else {
    private val name: String,
    private val values: Set<Direction>
    //?}
) //? if >=1.21.2 {
/*: BedrockProperty<Direction> {
*///?} else {
: DirectionProperty(name, values), BedrockProperty<Direction, BedrockDirectionProperty> {
//?}
    companion object {
        fun of(bedrockName: String, values: Set<Direction> = enumValues<Direction>().toSet()): BedrockDirectionProperty {
            //? if >=1.21.2 {
            /*// 使用Properties工厂方法创建标准Property
            val javaProperty = if (values == Direction.Type.HORIZONTAL.toSet()) {
                Properties.HORIZONTAL_FACING
            } else if (values.size == 6) {
                Properties.FACING
            } else {
                // 对于自定义值集，使用Properties.FACING并在运行时验证
                Properties.FACING
            }
            return BedrockDirectionProperty(bedrockName, javaProperty)
            *///?} else {
            val name = bedrockName
            return BedrockDirectionProperty(bedrockName, name, values)
            //?}
        }
    }

    //? if <1.21.2 {
    override val javaProperty get() = this
    //?}

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Direction): String = value.asString()
}