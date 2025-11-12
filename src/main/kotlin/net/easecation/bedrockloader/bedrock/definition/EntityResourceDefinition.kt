package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import net.minecraft.util.Identifier
import java.lang.reflect.Type

data class EntityResourceDefinition(
        @SerializedName("format_version") val formatVersion: String,
        @SerializedName("minecraft:client_entity") val clientEntity: ClientEntity
) {

    data class ClientEntity(
            val description: ClientEntityDescription
    )

    data class ClientEntityDescription(
            val animations: Map<String, String>?,
            val enable_attachables: Boolean?,
            val geometry: Map<String, String>?,
            val queryable_geometry: String?,
            val hide_armor: Boolean?,
            val held_item_ignores_lighting: Boolean?,
            val identifier: Identifier,
            val materials: Map<String, String>?,
            val min_engine_version: String?,
            val particle_effects: Map<String, String>?,
            val particle_emitters: Map<String, String>?,
            val render_controllers: List<RenderControllerReference>?,
            val scripts: Scripts?,
            val sound_effects: Map<String, String>?,
            val spawn_egg: SpawnEgg?,
            val textures: Map<String, String>?
    )

    data class RenderControllerReference(
            val id: String,
            val condition: String? = null
    ) {
        class Deserializer : JsonDeserializer<RenderControllerReference> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): RenderControllerReference {
                return when {
                    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                        // Format A: Simple string reference
                        // Example: "controller.render.zombie"
                        RenderControllerReference(json.asString, null)
                    }
                    json.isJsonObject -> {
                        // Format B: Object with condition
                        // Example: {"controller.render.dragon": "query.death_ticks > 1.0"}
                        val obj = json.asJsonObject
                        if (obj.entrySet().isEmpty()) {
                            throw JsonParseException("render_controller object cannot be empty")
                        }
                        val entry = obj.entrySet().first()
                        RenderControllerReference(entry.key, entry.value.asString)
                    }
                    else -> {
                        throw JsonParseException("Invalid render_controller format: expected string or object, got ${json.javaClass.simpleName}")
                    }
                }
            }
        }
    }

    data class Scripts(
            val animate: List<Any>?,
            val initialize: List<String>?,
            val pre_animation: List<String>?,
            val parent_setup: Any?,
            val scale: Any?,
            val scalex: Any?,
            val scaley: Any?,
            val scalez: Any?,
            val should_update_bones_and_effects_offscreen: Any?,
            val should_update_effects_offscreen: Any?,
            val variables: Map<String, String>?
    )

    data class SpawnEgg(
            val base_color: String?,
            val overlay_color: String?,
            val texture: String?,
            val texture_index: Int?
    )

}