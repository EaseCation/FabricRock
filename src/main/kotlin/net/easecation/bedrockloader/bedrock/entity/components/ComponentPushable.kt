package net.easecation.bedrockloader.bedrock.entity.components

data class ComponentPushable(
        val is_pushable: Boolean?,
        val is_pushable_by_piston: Boolean?,
) : IEntityComponent
