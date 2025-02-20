package net.easecation.bedrockloader.bedrock.block.state

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

sealed interface IBlockState {
    class Deserializer : JsonDeserializer<IBlockState> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IBlockState =
            when {
                json.isJsonObject -> context.deserialize(json, StateRange::class.java)
                json.isJsonArray -> json.asJsonArray.let { array ->
                    when {
                        !array.isEmpty && array[0].isJsonPrimitive -> array[0].asJsonPrimitive.let { first ->
                            when {
                                first.isBoolean -> context.deserialize<StateBoolean>(json, StateBoolean::class.java)
                                first.isString -> context.deserialize<StateString>(json, StateString::class.java)
                                first.isNumber -> context.deserialize<StateInt>(json, StateInt::class.java)
                                else -> throw JsonParseException("Unexpected JSON type for IBlockState")
                            }
                        }
                        else -> throw JsonParseException("Unexpected JSON type for IBlockState")
                    }
                }
                else -> throw JsonParseException("Unexpected JSON type for IBlockState")
            }
    }
}