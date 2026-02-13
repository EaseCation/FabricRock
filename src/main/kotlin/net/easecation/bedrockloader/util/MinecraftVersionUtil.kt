package net.easecation.bedrockloader.util

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.fabricmc.loader.api.VersionParsingException

object MinecraftVersionUtil {
    private val minecraftVersion: Version by lazy {
        FabricLoader.getInstance().getModContainer("minecraft")
            .orElseThrow { IllegalStateException("Minecraft mod container not found") }
            .metadata
            .version
    }

    /**
     * 获取当前 Minecraft 版本字符串
     * 例如: "1.21.1", "1.21.10", "1.21.11"
     */
    fun getVersionString(): String {
        return minecraftVersion.friendlyString
    }

    /**
     * 检查当前版本是否 >= 指定版本
     * 例如: isAtLeast("1.21.10") 在 1.21.11 上返回 true
     */
    fun isAtLeast(versionString: String): Boolean {
        return try {
            val targetVersion = Version.parse(versionString)
            minecraftVersion.compareTo(targetVersion) >= 0
        } catch (e: VersionParsingException) {
            false
        }
    }

    /**
     * 检查当前版本是否在指定范围内
     * 例如: isInRange("1.21.1", "1.21.11")
     */
    fun isInRange(minVersion: String, maxVersion: String): Boolean {
        return try {
            val min = Version.parse(minVersion)
            val max = Version.parse(maxVersion)
            minecraftVersion.compareTo(min) >= 0 && minecraftVersion.compareTo(max) <= 0
        } catch (e: VersionParsingException) {
            false
        }
    }

    /**
     * 获取 Fabric API 版本
     */
    fun getFabricApiVersion(): String? {
        return FabricLoader.getInstance().getModContainer("fabric-api")
            .map { it.metadata.version.friendlyString }
            .orElse(null)
    }
}
