package net.easecation.bedrockloader.bedrock.entity.components

data class ComponentPhysics(
        val has_collision: Boolean,
        val has_gravity: Boolean
) : IEntityComponent
