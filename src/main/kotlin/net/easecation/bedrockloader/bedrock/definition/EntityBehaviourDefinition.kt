package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.annotations.SerializedName
import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.minecraft.util.Identifier

data class EntityBehaviourDefinition(
        @SerializedName("format_version") val formatVersion: String,
        @SerializedName("minecraft:entity") val minecraftEntity: EntityBehaviour
) {

    data class EntityBehaviour(
            val description: Description,
            val components: EntityComponents,
            val component_groups: Map<String, Any>?,
            val events: Map<String, Any>?
    )

    data class Description(
            val identifier: Identifier,
            val is_spawnable: Boolean?,
            val is_summonable: Boolean?
    )

}
