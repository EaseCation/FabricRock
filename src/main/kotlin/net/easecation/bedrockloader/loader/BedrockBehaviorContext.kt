package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.definition.BlockBehaviourDefinition
import net.minecraft.util.Identifier

class BedrockBehaviorContext {

    val blocks: MutableMap<Identifier, BlockBehaviourDefinition.BlockBehaviour> = mutableMapOf()

}