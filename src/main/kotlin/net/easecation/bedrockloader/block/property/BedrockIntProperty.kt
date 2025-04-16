package net.easecation.bedrockloader.block.property

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.state.property.Property
import java.util.*
import java.util.function.Supplier

data class BedrockIntProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<Int>,
    private val specializedTag: Boolean
) : Property<Int>(bedrockName, Int::class.javaObjectType), BedrockProperty<Int, BedrockIntProperty> {
    companion object {
        fun of(bedrockName: String, values: Set<Int>, specializedTag: Boolean = true): BedrockIntProperty {
            val name = bedrockName
            return BedrockIntProperty(bedrockName, name, values, specializedTag)
        }
    }

    private val codec: Codec<Int> = if (!specializedTag) super.getCodec() else Codec.INT.comapFlatMap(
        {
            when {
                values.contains(it) -> DataResult.success(it)
                else -> DataResult.error{ "Unable to read property: $this with value: $it" }
            }
        },
        { it }
    )
    override fun getCodec(): Codec<Int> = codec
    private val valueCodec: Codec<Value<Int>> = if (!specializedTag) super.getValueCodec() else this.codec.xmap(this::createValue, Value<Int>::value)
    override fun getValueCodec(): Codec<Value<Int>> = valueCodec

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Int): String = value.toString()

    override fun getValues(): Collection<Int> = values

    override fun parse(name: String): Optional<Int> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Int): String = value.toString()
}