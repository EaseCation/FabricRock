package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.minecraft.client.util.SpriteIdentifier

data class BedrockMaterialInstance(
    val spriteId: SpriteIdentifier,
    val renderMethod: ComponentMaterialInstances.RenderMethod? = null
)