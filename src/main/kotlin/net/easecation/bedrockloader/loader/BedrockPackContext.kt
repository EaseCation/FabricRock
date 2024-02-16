package net.easecation.bedrockloader.loader

data class BedrockPackContext(
        val resource: BedrockResourceContext = BedrockResourceContext(),
        val behavior: BedrockBehaviorContext = BedrockBehaviorContext()
)
