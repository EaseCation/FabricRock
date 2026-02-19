package net.easecation.bedrockloader.sync.common

import com.google.gson.annotations.SerializedName

/**
 * 远程资源包同步清单
 *
 * 与基岩版包的 PackManifest 不同，这个类用于HTTP服务器和客户端之间的资源包同步
 *
 * 版本历史:
 * - 1.0: 初始版本，只支持单个包
 * - 2.0: 支持addon（.mcaddon文件），添加type字段
 */
/**
 * 加密配置（manifest 级别）
 */
data class EncryptionConfig(
    /** 是否启用加密 */
    @SerializedName("enabled")
    val enabled: Boolean = false,

    /** 加密算法 */
    @SerializedName("algorithm")
    val algorithm: String = "AES-256-CFB8",

    /** 服务端公开令牌（客户端用于派生 shared_secret） */
    @SerializedName("server_token")
    val serverToken: String = ""
)

data class RemotePackManifest(
    /**
     * 清单格式版本
     * - "1.0": 旧版本，不支持addon
     * - "2.0": 支持addon和type字段
     * - "3.0": 支持加密配置
     */
    @SerializedName("version")
    val version: String = "3.0",

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
     * 资源包列表（包含单个包和addon）
     */
    @SerializedName("packs")
    val packs: List<RemotePackInfo>,

    /**
     * 加密配置（可选）
     * 仅在服务端启用加密时包含
     */
    @SerializedName("encryption")
    val encryption: EncryptionConfig? = null
) {
    companion object {
        const val VERSION_1_0 = "1.0"
        const val VERSION_2_0 = "2.0"
        const val VERSION_3_0 = "3.0"
        const val CURRENT_VERSION = VERSION_3_0
    }

    /**
     * 服务端是否启用了加密
     */
    fun isEncrypted(): Boolean = encryption?.enabled == true

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

    /**
     * 获取所有addon类型的资源
     */
    fun getAddons(): List<RemotePackInfo> {
        return packs.filter { it.type == ResourceType.ADDON }
    }

    /**
     * 获取所有单包类型的资源
     */
    fun getSinglePacks(): List<RemotePackInfo> {
        return packs.filter { it.type == ResourceType.PACK }
    }

    /**
     * 检查是否为v2.0格式（支持addon）
     */
    fun supportsAddon(): Boolean {
        return version == VERSION_2_0
    }
}
