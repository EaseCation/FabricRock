package net.easecation.bedrockloader.bedrock.pack

import com.google.gson.*
import java.lang.reflect.Type

data class SemVersion(
        val major: Int,
        val minor: Int,
        val patch: Int
) {
    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    class Serializer : JsonSerializer<SemVersion>, JsonDeserializer<SemVersion> {
        override fun serialize(src: SemVersion, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonArr = JsonArray()
            jsonArr.add(src.major)
            jsonArr.add(src.minor)
            jsonArr.add(src.patch)
            return jsonArr
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SemVersion {
            if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
                val version = json.asString.split(".")
                return SemVersion(version[0].toInt(), version[1].toInt(), version[2].toInt())
            } else {
                val jsonArr = json.asJsonArray
                return SemVersion(jsonArr[0].asInt, jsonArr[1].asInt, jsonArr[2].asInt)
            }
        }
    }
}