package net.easecation.bedrockloader.loader.deserializer

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

}