package net.easecation.bedrockloader.bedrock.entity.components

data class ComponentHealth(
        val value: Float?,
        val max: Float?,
        val min: Float?
) : IEntityComponent
