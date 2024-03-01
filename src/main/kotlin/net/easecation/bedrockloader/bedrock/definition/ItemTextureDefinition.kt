package net.easecation.bedrockloader.bedrock.definition

import net.easecation.bedrockloader.bedrock.BedrockTexturePath

data class ItemTextureDefinition(
        val resource_pack_name: String?,
        val texture_name: String?,
        val padding: Int?,
        val num_mip_levels: Int?,
        val texture_data: Map<String, TextureData>
) {
    data class TextureData(
        val textures: BedrockTexturePath
    )
}
