package net.easecation.bedrockloader.util

import com.google.gson.*
import net.easecation.bedrockloader.bedrock.block.component.*
import net.easecation.bedrockloader.bedrock.block.state.IBlockState
import net.easecation.bedrockloader.bedrock.pack.SemVersion
import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.GeometryDefinition
import net.easecation.bedrockloader.bedrock.definition.TextureDataDefinition
import net.easecation.bedrockloader.bedrock.entity.components.ComponentRideable
import net.minecraft.util.Identifier
import java.lang.reflect.Type
import java.util.UUID

/**
 * 将驼峰命名转换为下划线命名，并转换为小写
 * 例如: pistonArmCollision -> piston_arm_collision
 */
fun String.camelToSnakeCase(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}

/**
 * 规范化基岩版标识符为Java版兼容格式
 * 将驼峰命名转换为下划线命名，确保符合Minecraft Identifier规范
 */
fun String.normalizeIdentifier(): String {
    return this.camelToSnakeCase()
}

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
            .registerTypeAdapter(TextureDataDefinition::class.java, TextureDataDefinition.Deserializer())
            // block state
            .registerTypeAdapter(IBlockState::class.java, IBlockState.Deserializer())
            // block component - 使用 Factory 而不是 Deserializer，避免无限递归
            .registerTypeAdapterFactory(BlockComponents.Factory())
            .registerTypeAdapter(ComponentGeometry::class.java, ComponentGeometry.Deserializer())
            .registerTypeAdapter(ComponentSelectionBox::class.java, ComponentSelectionBox.Deserializer())
            .registerTypeAdapter(ComponentCollisionBox::class.java, ComponentCollisionBox.Deserializer())
            .registerTypeAdapter(ComponentPlacementFilter::class.java, ComponentPlacementFilter.Deserializer())
            .registerTypeAdapter(ComponentTransformation::class.java, ComponentTransformation.Deserializer())
            .registerTypeAdapter(ComponentMapColor::class.java, ComponentMapColor.Deserializer())
            // entity component
            .registerTypeAdapter(ComponentRideable::class.java, ComponentRideable.Deserializer())
            // entity resource
            .registerTypeAdapter(EntityResourceDefinition.RenderControllerReference::class.java,
                    EntityResourceDefinition.RenderControllerReference.Deserializer())
            // geometry
            .registerTypeAdapter(GeometryDefinition.Uv::class.java, GeometryDefinition.Uv.Deserializer())
            .registerTypeAdapter(GeometryDefinition.Locator::class.java, GeometryDefinition.Locator.Deserializer())
            // animation
            .registerTypeAdapter(AnimationDefinition.LoopMode::class.java, AnimationDefinition.LoopMode.Deserializer())
            .registerTypeAdapter(AnimationDefinition.BoneAnimation::class.java, AnimationDefinition.BoneAnimation.Deserializer())
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
            return Identifier.of(json.asString.normalizeIdentifier())
        }
    }
}