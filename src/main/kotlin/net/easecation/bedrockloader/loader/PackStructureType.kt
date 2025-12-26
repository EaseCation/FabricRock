package net.easecation.bedrockloader.loader

/**
 * 包结构类型枚举
 * 用于识别不同的包/addon目录结构
 */
enum class PackStructureType {
    /**
     * 单个包（目录有manifest.json）
     */
    SINGLE_PACK,

    /**
     * Addon目录（目录无manifest.json但子目录有）
     */
    ADDON_DIRECTORY,

    /**
     * .mcaddon文件（包含多个包的ZIP）
     */
    MCADDON_FILE,

    /**
     * .mcpack或.zip文件（单个包）
     */
    MCPACK_FILE,

    /**
     * 无法识别的结构
     */
    UNKNOWN
}
