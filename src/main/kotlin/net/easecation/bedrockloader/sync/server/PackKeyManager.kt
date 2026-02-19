package net.easecation.bedrockloader.sync.server

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.easecation.bedrockloader.sync.common.PackEncryption
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * 资源包加密密钥管理器
 *
 * 为每个资源包文件管理独立的 AES-256 加密密钥。
 * 密钥持久化到 .keys.json 文件中，服务端重启后保持一致。
 */
class PackKeyManager(private val keyFile: File) {

    private val logger = LoggerFactory.getLogger("BedrockLoader/PackKeyManager")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val keys = mutableMapOf<String, String>()

    init {
        load()
    }

    /**
     * 获取指定包的加密密钥，如果不存在则自动生成
     * @param filename 包文件名
     * @return 64字符十六进制密钥
     */
    fun getOrCreateKey(filename: String): String {
        return keys.getOrPut(filename) {
            val newKey = PackEncryption.generateKey()
            logger.info("Generated new encryption key for: $filename")
            save()
            newKey
        }
    }

    /**
     * 重新生成指定包的密钥（当包文件更新时调用）
     */
    fun regenerateKey(filename: String): String {
        val newKey = PackEncryption.generateKey()
        keys[filename] = newKey
        logger.info("Regenerated encryption key for: $filename")
        save()
        return newKey
    }

    /**
     * 获取所有密钥映射
     */
    fun getAllKeys(): Map<String, String> = keys.toMap()

    /**
     * 保存密钥到文件
     */
    fun save() {
        try {
            keyFile.parentFile?.mkdirs()
            Files.newBufferedWriter(keyFile.toPath(), StandardCharsets.UTF_8).use { writer ->
                gson.toJson(keys, writer)
            }
        } catch (e: Exception) {
            logger.error("Failed to save key file: ${keyFile.absolutePath}", e)
        }
    }

    /**
     * 从文件加载密钥
     */
    fun load() {
        if (!keyFile.exists()) {
            logger.debug("Key file not found, will create on first use: ${keyFile.absolutePath}")
            return
        }

        try {
            Files.newBufferedReader(keyFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<Map<String, String>>() {}.type
                val loaded: Map<String, String> = gson.fromJson(reader, type) ?: emptyMap()
                keys.clear()
                keys.putAll(loaded)
                logger.info("Loaded ${keys.size} encryption key(s) from ${keyFile.name}")
            }
        } catch (e: Exception) {
            logger.error("Failed to load key file: ${keyFile.absolutePath}", e)
        }
    }
}
