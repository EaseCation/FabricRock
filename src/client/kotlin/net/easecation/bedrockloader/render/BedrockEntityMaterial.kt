package net.easecation.bedrockloader.render

/**
 * 基岩版实体材质类型枚举
 *
 * 根据 vanilla/materials/entity.material 文件定义的材质类型，
 * 硬编码映射到 Java 版的渲染方式。
 */
enum class BedrockEntityMaterial(
    val alphaTest: Boolean = false,
    val blending: Boolean = false,
    val emissive: Boolean = false,
    val disableCulling: Boolean = false
) {
    /** 标准实体渲染 */
    ENTITY,

    /** Alpha 测试（镂空），禁用背面剔除（双面渲染） */
    ENTITY_ALPHATEST(alphaTest = true, disableCulling = true),

    /** Alpha 测试（镂空），单面渲染（正常背面剔除） */
    ENTITY_ALPHATEST_ONE_SIDED(alphaTest = true),

    /** Alpha 混合（半透明） */
    ENTITY_ALPHABLEND(blending = true),

    /** 自发光 */
    ENTITY_EMISSIVE(emissive = true),

    /** 自发光 + Alpha 测试，双面渲染 */
    ENTITY_EMISSIVE_ALPHA(emissive = true, alphaTest = true, disableCulling = true),

    /** 自发光 + Alpha 测试，单面渲染 */
    ENTITY_EMISSIVE_ALPHA_ONE_SIDED(emissive = true, alphaTest = true),

    /** 禁用背面剔除 */
    ENTITY_NOCULL(disableCulling = true);

    companion object {
        /**
         * 硬编码映射：基岩版材质名 → 枚举值
         *
         * 基于 entity.material 文件中的定义：
         * - entity_alphatest:entity_nocull -> 双面渲染（DisableCulling）
         * - entity_alphatest_one_sided:entity -> 单面渲染（正常剔除）
         * - entity_emissive_alpha:entity_nocull -> 双面渲染
         * - entity_emissive_alpha_one_sided:entity -> 单面渲染
         */
        private val MATERIAL_MAP = mapOf(
            // 基础材质类型
            "entity" to ENTITY,
            "entity_static" to ENTITY,
            "entity_alphatest" to ENTITY_ALPHATEST,                          // 双面
            "entity_alphatest_one_sided" to ENTITY_ALPHATEST_ONE_SIDED,      // 单面
            "entity_alphablend" to ENTITY_ALPHABLEND,
            "entity_alphablend_nocolor" to ENTITY_ALPHABLEND,
            "entity_emissive" to ENTITY_EMISSIVE,
            "entity_emissive_alpha" to ENTITY_EMISSIVE_ALPHA,                // 双面
            "entity_emissive_alpha_one_sided" to ENTITY_EMISSIVE_ALPHA_ONE_SIDED,  // 单面
            "entity_nocull" to ENTITY_NOCULL,
            "entity_change_color" to ENTITY_NOCULL,

            // 常见实体特定材质（从 entity.material 文件末尾的映射）
            "blaze_head" to ENTITY_EMISSIVE_ALPHA,
            "blaze_body" to ENTITY_EMISSIVE,
            "spider" to ENTITY_EMISSIVE_ALPHA,
            "spider_invisible" to ENTITY_EMISSIVE_ALPHA,
            "enderman" to ENTITY_EMISSIVE_ALPHA,
            "enderman_invisible" to ENTITY_EMISSIVE_ALPHA,
            "phantom" to ENTITY_EMISSIVE_ALPHA,
            "phantom_invisible" to ENTITY_EMISSIVE_ALPHA,
            "magma_cube" to ENTITY_EMISSIVE_ALPHA,
            "ghast" to ENTITY_EMISSIVE_ALPHA,
            "drowned" to ENTITY_EMISSIVE_ALPHA,
            "glow_squid" to ENTITY_EMISSIVE,

            // 其他常见材质
            "slime_outer" to ENTITY_ALPHABLEND,
            "guardian_ghost" to ENTITY_ALPHABLEND,
            "player_spectator" to ENTITY_ALPHABLEND,

            // 默认
            "default" to ENTITY
        )

        /**
         * 从基岩版材质名称获取对应的材质枚举
         *
         * @param name 基岩版材质名称，可能带有 "Material." 或 "material." 前缀
         * @return 对应的材质枚举，如果未找到则返回 ENTITY
         */
        fun fromBedrockName(name: String): BedrockEntityMaterial {
            // 去掉 "Material." 或 "material." 前缀
            val cleanName = name
                .removePrefix("Material.")
                .removePrefix("material.")
                .lowercase()
            return MATERIAL_MAP[cleanName] ?: ENTITY
        }
    }
}
