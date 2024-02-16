package net.easecation.bedrockloader.bedrock.pack

abstract class AbstractBedrockPack : BedrockPack {

    protected var manifest: PackManifest? = null

    protected var id: String? = null
    protected var version: String? = null
    protected var type: String? = null

    override fun getPackName(): String? {
        return manifest?.header?.name
    }

    override fun getPackId(): String? {
        return id
    }

    override fun getPackVersion(): String? {
        return version
    }

    override fun getPackType(): String? {
        return type
    }

    override fun getEncryptionKey(): String {
        return ""
    }

}