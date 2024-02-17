package net.easecation.bedrockloader.bedrock.block.component

/**
 * Custom Blocks: Render and Lighting Options
 * https://learn.microsoft.com/en-us/minecraft/creator/documents/customblockrenderlighting?view=minecraft-bedrock-stable
 */
class ComponentMaterialInstances : LinkedHashMap<String, ComponentMaterialInstances.Instance>(), IBlockComponent {

    data class Instance(
            val ambient_occlusion: Boolean?,
            val face_dimming: Boolean?,
            val render_method: RenderMethod?,
            val texture: String?
    )

    enum class RenderMethod {
        opaque, // 用于没有 Alpha 层的常规块纹理。不允许透明或半透明。
        double_sided, // 用于完全禁用背面剔除
        blend, // 用于像彩色玻璃这样的块。允许透明度和半透明（稍微透明的纹理）
        alpha_test // 用于像vanilla（未染色）玻璃这样的方块。不允许半透明，仅允许完全不透明或完全透明的纹理。还禁用背面剔除。
    }

}