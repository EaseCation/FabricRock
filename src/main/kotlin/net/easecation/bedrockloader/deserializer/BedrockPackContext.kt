package net.easecation.bedrockloader.deserializer

/**
 * Mixed all content of loaded bedrock pack
 * 混合了所有加载的基岩材质包和行为包的内容，用于loader的交叉读取
 */
data class BedrockPackContext(
        val resource: BedrockResourceContext = BedrockResourceContext(),
        val behavior: BedrockBehaviorContext = BedrockBehaviorContext()
)
