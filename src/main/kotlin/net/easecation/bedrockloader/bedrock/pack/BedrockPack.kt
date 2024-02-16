package net.easecation.bedrockloader.bedrock.pack

interface BedrockPack {

    fun getPackName(): String?

    fun getPackId(): String?

    fun getPackVersion(): String?

    fun getPackType(): String?

    fun getEncryptionKey(): String?
}