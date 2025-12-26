package net.easecation.bedrockloader.sync.common

import com.google.gson.annotations.SerializedName

/**
 * 资源类型枚举
 */
enum class ResourceType {
    /**
     * Addon包（.mcaddon文件，包含多个子包）
     */
    @SerializedName("addon")
    ADDON,

    /**
     * 单个包（.zip/.mcpack文件）
     */
    @SerializedName("pack")
    PACK
}

/**
 * 单个远程资源的信息
 * 可以是单个包（.zip/.mcpack）或addon（.mcaddon）
 */
data class RemotePackInfo(
    /**
     * 文件名（包含扩展名）
     */
    @SerializedName("name")
    val name: String,

    /**
     * 资源类型：ADDON 或 PACK
     * 默认为 PACK 以保持向后兼容
     */
    @SerializedName("type")
    val type: ResourceType = ResourceType.PACK,

    /**
     * 包的UUID（从manifest.json的header.uuid提取）
     * 用于冲突检测和智能去重
     * 对于ADDON类型，这是第一个子包的UUID
     */
    @SerializedName("uuid")
    val uuid: String? = null,

    /**
     * 包的版本（从manifest.json的header.version提取）
     */
    @SerializedName("version")
    val version: String? = null,

    /**
     * 文件的MD5哈希值（32位十六进制字符串）
     */
    @SerializedName("md5")
    val md5: String,

    /**
     * 文件大小（字节）
     */
    @SerializedName("size")
    val size: Long,

    /**
     * 下载URL（相对或绝对路径）
     */
    @SerializedName("url")
    val url: String
) {
    /**
     * 判断是否为addon类型
     */
    fun isAddon(): Boolean = type == ResourceType.ADDON

    /**
     * 判断是否为.mcaddon文件
     */
    fun isMcAddon(): Boolean = name.endsWith(".mcaddon", ignoreCase = true)
}
