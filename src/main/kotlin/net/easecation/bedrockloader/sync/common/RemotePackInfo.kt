package net.easecation.bedrockloader.sync.common

import com.google.gson.annotations.SerializedName

/**
 * 单个远程资源包的信息
 */
data class RemotePackInfo(
    /**
     * 文件名（包含扩展名）
     */
    @SerializedName("name")
    val name: String,

    /**
     * 包的UUID（从manifest.json的header.uuid提取）
     * 用于冲突检测和智能去重
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
)
