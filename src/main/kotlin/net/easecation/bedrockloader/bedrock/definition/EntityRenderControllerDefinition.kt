package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.annotations.SerializedName
import net.minecraft.util.Identifier

data class EntityRenderControllerDefinition(
        @SerializedName("format_version") val formatVersion: String,
        @SerializedName("render_controllers") val renderControllers: Map<String, RenderController>
) {

    data class RenderController(
            val arrays: Arrays?,
            val color: Color?,
            val filter_lighting: Boolean?,
            val geometry: String,
            val ignore_lighting: Boolean?,
            val is_hurt_color: IsHurtColor?,
            val light_color_multiplier: Any?,
            val materials: List<Map<String, String>>,
            val on_fire_color: OnFireColor?,
            val overlay_color: OverlayColor?,
            val part_visibility: List<Map<String, Any>>?,
            val rebuild_animation_matrices: Boolean?,
            val textures: List<String>?,
            val uv_anim: UvAnim?
    )

    data class Arrays(
            val geometries: Map<String, List<String>>?,
            val materials: Map<String, List<String>>?,
            val textures: Map<String, List<String>>?
    )

    data class Color(
            val r: Any?,
            val g: Any?,
            val b: Any?,
            val a: Any?
    )

    data class IsHurtColor(
            val r: Any?,
            val g: Any?,
            val b: Any?,
            val a: Any?
    )

    data class OnFireColor(
            val r: Any?,
            val g: Any?,
            val b: Any?,
            val a: Any?
    )

    data class OverlayColor(
            val r: Any?,
            val g: Any?,
            val b: Any?,
            val a: Any?
    )

    data class UvAnim(
            val offset: List<Any>,
            val scale: List<Any>
    )

}