package net.easecation.bedrockloader.loader.deserializer

import net.easecation.bedrockloader.loader.InMemoryZipPack
import java.util.zip.ZipFile

interface PackDeserializer<T> {

    fun deserialize(file: ZipFile): T

    /**
     * 从ZIP文件中反序列化，支持路径前缀
     * 用于.mcaddon文件中的子包加载
     *
     * @param file ZIP文件
     * @param pathPrefix 路径前缀（如 "behavior_pack/"），所有路径都需要以此开头
     */
    fun deserialize(file: ZipFile, pathPrefix: String): T

    /**
     * 从内存ZIP包中反序列化
     * 用于加密包解密后的内存加载
     *
     * @param pack 内存中的ZIP包
     */
    fun deserialize(pack: InMemoryZipPack): T

    /**
     * 从内存ZIP包中反序列化，支持路径前缀
     * 用于加密的.mcaddon文件解密后的内存加载
     *
     * @param pack 内存中的ZIP包
     * @param pathPrefix 路径前缀（如 "behavior_pack/"）
     */
    fun deserialize(pack: InMemoryZipPack, pathPrefix: String): T

}