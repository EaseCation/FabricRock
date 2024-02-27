package net.easecation.bedrockloader.util

import com.google.gson.*
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.bedrock.block.component.ComponentPlacementFilter
import net.easecation.bedrockloader.bedrock.block.component.ComponentSelectionBox
import net.easecation.bedrockloader.bedrock.pack.SemVersion
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.bedrock.entity.components.ComponentRideable
import net.minecraft.util.Identifier
import java.lang.reflect.Type
import java.util.UUID

object GsonUtil {

    val GSON: Gson = GsonBuilder()
            .setPrettyPrinting()
            // basic
            .registerTypeAdapter(UUID::class.java, UUIDSerializer())
            .registerTypeAdapter(Identifier::class.java, IdentifierSerializer())
            .registerTypeAdapter(SemVersion::class.java, SemVersion.Serializer())
            // file
            .registerTypeAdapter(BlockResourceDefinition::class.java, BlockResourceDefinition.Deserializer())
            .registerTypeAdapter(BlockResourceDefinition.Textures::class.java, BlockResourceDefinition.Textures.Deserializer())
            // block component
            .registerTypeAdapter(ComponentGeometry::class.java, ComponentGeometry.Deserializer())
            .registerTypeAdapter(ComponentSelectionBox::class.java, ComponentSelectionBox.Deserializer())
            .registerTypeAdapter(ComponentCollisionBox::class.java, ComponentCollisionBox.Deserializer())
            .registerTypeAdapter(ComponentPlacementFilter::class.java, ComponentPlacementFilter.Deserializer())
            // entity component
            .registerTypeAdapter(ComponentRideable::class.java, ComponentRideable.Deserializer())
            // geometry
            .registerTypeAdapter(GeometryDefinition.Uv::class.java, GeometryDefinition.Uv.Deserializer())
            .create()

    class UUIDSerializer : JsonSerializer<UUID>, JsonDeserializer<UUID> {
        override fun serialize(src: UUID, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UUID {
            return UUID.fromString(json.asString)
        }
    }

    class IdentifierSerializer : JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
        override fun serialize(src: Identifier, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString())
        }
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Identifier {
            return Identifier(json.asString)
        }
    }
}