package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.annotations.SerializedName
import net.minecraft.util.Identifier

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
            val render_controllers: List<String>?,
            val scripts: Scripts?,
            val sound_effects: Map<String, String>?,
            val spawn_egg: SpawnEgg?,
            val textures: Map<String, String>?
    )

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