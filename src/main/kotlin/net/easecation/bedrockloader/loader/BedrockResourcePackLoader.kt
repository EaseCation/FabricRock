package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.java.definition.JavaBlockStatesDefinition
import net.easecation.bedrockloader.java.definition.JavaMCMeta
import net.easecation.bedrockloader.java.definition.JavaModelDefinition
import net.easecation.bedrockloader.util.GsonUtil
import net.minecraft.util.Identifier
import java.io.File
import java.io.FileWriter
import javax.imageio.ImageIO

class BedrockResourcePackLoader(
        private val javaResDir: File,
        private val context: BedrockResourceContext
) {

    private val initedNamespaces = mutableSetOf<String>()

    private fun init() {
        javaResDir.deleteRecursively()
        javaResDir.mkdirs()
        // pack.mcmeta
        val fileMcMeta = javaResDir.resolve("pack.mcmeta")
        val mcMeta = JavaMCMeta(
                pack = JavaMCMeta.PackInfo(
                        pack_format = 8,
                        description = "Bedrock addons loader"
                )
        )
        FileWriter(fileMcMeta).use { writer ->
            GsonUtil.GSON.toJson(mcMeta, writer)
        }
        // pack.png
        val filePackIcon = javaResDir.resolve("pack.png")
        val packIcon = BedrockLoader::class.java.getResourceAsStream("/res-pack.png")
        packIcon.use { input ->
            filePackIcon.outputStream().use { output ->
                input?.copyTo(output) ?: throw NullPointerException("Cannot find resource: res-pack.png")
            }
        }
        // 创建assets文件夹
        val assetsDir = javaResDir.resolve("assets")
        assetsDir.mkdirs()
    }

    private fun namespaceDir(namespace: String) : File {
        val namespaceDir = javaResDir.resolve("assets").resolve(namespace)
        if (!namespaceDir.exists()) {
            namespaceDir.mkdirs()
        }
        // init dirs
        if (!initedNamespaces.contains(namespace)) {
            val blockStates = namespaceDir.resolve("blockstates")
            if (!blockStates.exists()) {
                blockStates.mkdirs()
            }
            val models = namespaceDir.resolve("models")
            if (!models.exists()) {
                models.mkdirs()
            }
            val modelsBlock = models.resolve("block")
            if (!modelsBlock.exists()) {
                modelsBlock.mkdirs()
            }
            val modelsItem = models.resolve("item")
            if (!modelsItem.exists()) {
                modelsItem.mkdirs()
            }
            val textures = namespaceDir.resolve("textures")
            if (!textures.exists()) {
                textures.mkdirs()
            }
            val texturesBlock = textures.resolve("block")
            if (!texturesBlock.exists()) {
                texturesBlock.mkdirs()
            }
            initedNamespaces.add(namespace)
        }
        return namespaceDir
    }

    fun load() {
        this.init()
        // Blocks
        for (block in context.blocks) {
            val identifier = block.key
            val dir = this.namespaceDir(identifier.namespace)
            createBlockTextures(dir, block.value.textures)
            createBlockModel(dir.resolve("models/block/${identifier.path}.json"), identifier, block.value.textures)
            createItemModel(dir.resolve("models/item/${identifier.path}.json"), identifier)
            createBlockState(dir.resolve("blockstates/${identifier.path}.json"), identifier)
        }
    }

    private fun createBlockState(file: File, identifier: Identifier) {
        val blockState = JavaBlockStatesDefinition(
                variants = mapOf(
                        "" to JavaBlockStatesDefinition.Variant("${identifier.namespace}:block/${identifier.path}")
                )
        )
        FileWriter(file).use { writer ->
            GsonUtil.GSON.toJson(blockState, writer)
        }
    }

    private fun createBlockTextures(namespaceDir: File, textures: BlockResourceDefinition.Textures) {
        when (textures) {
            is BlockResourceDefinition.Textures.TexturesAllFace -> {
                val texture = context.terrainTexture[textures.all]?.textures
                if (texture == null || !texture.contains("textures/")) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                    return
                }
                val bedrockTexture = context.textureImages[texture]
                if (bedrockTexture == null) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                    return
                }
                val file = namespaceDir.resolve(texture + "." + bedrockTexture.type.getExtension())
                bedrockTexture.image.let { image ->
                    ImageIO.write(image, file.extension, file)
                }
            }
            is BlockResourceDefinition.Textures.TexturesMultiFace -> {
                val directions = mapOf(
                        "up" to textures.up,
                        "down" to textures.down,
                        "north" to textures.north,
                        "south" to textures.south,
                        "east" to textures.east,
                        "west" to textures.west
                )
                for ((_, textureKey) in directions) {
                    textureKey?.let {
                        val texture = context.terrainTexture[it]?.textures
                        if (texture == null || !texture.contains("textures/")) {
                            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $it")
                            return
                        }
                        val bedrockTexture = context.textureImages[texture]
                        if (bedrockTexture == null) {
                            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $it")
                            return
                        }
                        val file = namespaceDir.resolve(texture + "." + bedrockTexture.type.getExtension())
                        bedrockTexture.image.let { image ->
                            ImageIO.write(image, file.extension, file)
                        }
                    }
                }
            }
        }
    }

    private fun createBlockModel(file: File, identifier: Identifier, textures: BlockResourceDefinition.Textures) {

        fun addTextureToMap(texturesMap: MutableMap<String, String>, direction: String, textureKey: String, identifier: Identifier) {
            val texture = context.terrainTexture[textureKey]?.textures
            if (texture == null || !texture.contains("textures/")) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $textureKey")
                return
            }
            texturesMap[direction] = texture.replace("textures/", "${identifier.namespace}:")
        }

        when (textures) {
            is BlockResourceDefinition.Textures.TexturesAllFace -> {
                val texture = context.terrainTexture[textures.all]?.textures
                if (texture == null || !texture.contains("textures/")) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                    return
                }
                val model = JavaModelDefinition(
                        parent = "block/cube_all",
                        textures = mapOf(
                                "all" to texture.replace("textures/", "${identifier.namespace}:")
                        )
                )
                FileWriter(file).use { writer ->
                    GsonUtil.GSON.toJson(model, writer)
                }
            }
            is BlockResourceDefinition.Textures.TexturesMultiFace -> {
                val texturesMap = mutableMapOf<String, String>()
                val directions = mapOf(
                        "up" to textures.up,
                        "down" to textures.down,
                        "north" to textures.north,
                        "south" to textures.south,
                        "east" to textures.east,
                        "west" to textures.west
                )

                for ((direction, textureKey) in directions) {
                    textureKey?.let {
                        addTextureToMap(texturesMap, direction, it, identifier)
                    }
                }

                val model = JavaModelDefinition(
                        parent = "block/cube",
                        textures = texturesMap
                )
                FileWriter(file).use { writer ->
                    GsonUtil.GSON.toJson(model, writer)
                }
            }
        }
    }

    private fun createItemModel(file: File, identifier: Identifier) {
        val model = JavaModelDefinition(
                parent = "${identifier.namespace}:block/${identifier.path}"
        )
        FileWriter(file).use { writer ->
            GsonUtil.GSON.toJson(model, writer)
        }
    }

}