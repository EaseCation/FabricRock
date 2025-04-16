package net.easecation.bedrockloader.block.property

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.state.property.Property
import java.util.*

data class BedrockBooleanProperty(
    private val bedrockName: String,
    private val name: String,
    private val values: Set<Boolean>,
    private val specializedTag: Boolean
) : Property<Boolean>(bedrockName, Boolean::class.javaObjectType), BedrockProperty<Boolean, BedrockBooleanProperty> {
    companion object {
        fun of(bedrockName: String, values: Set<Boolean>, specializedTag: Boolean = true): BedrockBooleanProperty {
            val name = bedrockName
            return BedrockBooleanProperty(bedrockName, name, values, specializedTag)
        }
    }

    private val codec: Codec<Boolean> = if (!specializedTag) super.getCodec() else Codec.BOOL.comapFlatMap(
        {
            when {
                values.contains(it) -> DataResult.success(it)
                else -> DataResult.error{ "Unable to read property: $this with value: $it" }
            }
        },
        { it }
    )
    override fun getCodec(): Codec<Boolean> = codec
    private val valueCodec: Codec<Value<Boolean>> = if (!specializedTag) super.getValueCodec() else this.codec.xmap(this::createValue, Value<Boolean>::value)
    override fun getValueCodec(): Codec<Value<Boolean>> = valueCodec

    override val javaProperty get() = this

    override fun getBedrockName(): String = bedrockName

    override fun getBedrockValueName(value: Boolean): String = value.toString()

    override fun getValues(): Collection<Boolean> = values

    override fun parse(name: String): Optional<Boolean> = Optional.ofNullable(values.find { it.toString() == name })

    override fun name(value: Boolean): String = value.toString()
}