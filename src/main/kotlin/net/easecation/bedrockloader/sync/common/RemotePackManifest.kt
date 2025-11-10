package net.easecation.bedrockloader.sync.common

import com.google.gson.annotations.SerializedName

/**
 * 远程资源包同步清单
 *
 * 与基岩版包的 PackManifest 不同，这个类用于HTTP服务器和客户端之间的资源包同步
 */
data class RemotePackManifest(
    /**
     * 清单格式版本
     */
    @SerializedName("version")
    val version: String = "1.0",

    /**
     * 清单生成时间戳（Unix时间戳，毫秒）
     */
    @SerializedName("generated_at")
    val generatedAt: Long,

    /**
     * 服务器版本信息（可选）
     */
    @SerializedName("server_version")
    val serverVersion: String? = null,

    /**
     * 资源包列表
     */
    @SerializedName("packs")
    val packs: List<RemotePackInfo>
) {
    /**
     * 获取包的总数量
     */
    fun getPackCount(): Int = packs.size

    /**
     * 获取所有包的总大小（字节）
     */
    fun getTotalSize(): Long = packs.sumOf { it.size }

    /**
     * 根据文件名查找包信息
     */
    fun findPackByName(name: String): RemotePackInfo? {
        return packs.find { it.name == name }
    }
}
