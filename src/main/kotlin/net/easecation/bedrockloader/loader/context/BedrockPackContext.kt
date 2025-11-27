package net.easecation.bedrockloader.loader.context

import net.easecation.bedrockloader.loader.BedrockPackRegistry

/**
 * 单个包的完整上下文
 * 保留每个包的独立数据，用于按包注册和管理
 */
data class SinglePackContext(
    val packId: String,                           // 包UUID
    val packInfo: BedrockPackRegistry.PackInfo,   // 包元数据
    val resource: BedrockResourceContext = BedrockResourceContext(),
    val behavior: BedrockBehaviorContext = BedrockBehaviorContext()
)

/**
 * Mixed all content of loaded bedrock pack
 * 混合了所有加载的基岩材质包和行为包的内容，用于loader的交叉读取
 *
 * 向后兼容设计：
 * - packs 存储每个包的独立上下文
 * - resource 和 behavior 通过 lazy 提供全局合并的视图，保持现有代码兼容
 */
data class BedrockPackContext(
    val packs: MutableList<SinglePackContext> = mutableListOf()
) {
    // 向后兼容：提供全局合并的视图
    val resource: BedrockResourceContext by lazy {
        val merged = BedrockResourceContext()
        packs.forEach { merged.putAll(it.resource) }
        merged
    }

    val behavior: BedrockBehaviorContext by lazy {
        val merged = BedrockBehaviorContext()
        packs.forEach { merged.putAll(it.behavior) }
        merged
    }
}
