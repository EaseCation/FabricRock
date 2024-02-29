package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.render.RenderControllerWithClientEntity
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.easecation.bedrockloader.deserializer.BedrockPackContext
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.render.renderer.EntityDataDrivenRenderer
import net.easecation.bedrockloader.java.definition.JavaBlockStatesDefinition
import net.easecation.bedrockloader.java.definition.JavaMCMeta
import net.easecation.bedrockloader.java.definition.JavaModelDefinition
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.easecation.bedrockloader.util.GsonUtil
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.entity.EntityType
import net.minecraft.util.Identifier
import java.io.File
import java.io.FileWriter
import javax.imageio.ImageIO

class BedrockResourcePackLoader(
        private val javaResDir: File,
        private val context: BedrockPackContext
) {

    private val initedNamespaces = mutableSetOf<String>()

    fun load() {
        val env = FabricLoader.getInstance().environmentType
        this.init()
        // Geometry
        context.resource.geometries.forEach { (key, value) ->
            BedrockAddonsRegistry.models[key] = BedrockGeometryModel(value)
        }
        // Blocks
        for (block in context.resource.blocks) {
            val identifier = block.key
            val dir = namespaceDir(identifier.namespace)
            createBlockTextures(identifier, block.value.textures, context.behavior.blocks[identifier]?.components?.minecraftMaterialInstances)
            createBlockModel(
                    dir.resolve("models/block/${identifier.path}.json"),
                    identifier,
                    block.value.textures,
                    context.behavior.blocks[identifier]?.components?.minecraftGeometry,
                    context.behavior.blocks[identifier]?.components?.minecraftMaterialInstances,
            )
            createItemModel(dir.resolve("models/item/${identifier.path}.json"), identifier)
            createBlockState(
                dir.resolve("blockstates/${identifier.path}.json"),
                identifier,
                context.behavior.blocks[identifier]?.components?.minecraftGeometry,
                context.behavior.blocks[identifier]?.components?.minecraftMaterialInstances
            )
        }
        // Entity
        for (entity in context.resource.entities) {
            val identifier = entity.key
            val clientEntity = entity.value.description
            val entityType = BedrockAddonsRegistry.getOrRegisterEntityType(identifier)
            // textures
            createEntityTextures(identifier, clientEntity)
            // renderer
            if (env == EnvType.CLIENT) {
                registerRenderController(clientEntity, identifier, entityType)
            }
        }
    }

    /**
     * 初始化临时资源包文件夹
     */
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

    /**
     * 获取命名空间对应的资源包文件夹，如果不存在则创建
     */
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
            val texturesBlocks = textures.resolve("blocks")
            if (!texturesBlocks.exists()) {
                texturesBlocks.mkdirs()
            }
            val texturesEntity = textures.resolve("entity")
            if (!texturesEntity.exists()) {
                texturesEntity.mkdirs()
            }
            initedNamespaces.add(namespace)
        }
        return namespaceDir
    }

    /**
     * 创建一个方块状态文件，内部包含model路径（附带命名空间）
     * 如果方块带模型，则详见createBlockModel，在创建的方块模型中，继承与自定义基岩版geometry模型，从而调用自定义渲染器和烘焙器来渲染基岩版模型
     */
    private fun createBlockState(
        file: File,
        identifier: Identifier,
        geometry: ComponentGeometry?,
        materialInstances: ComponentMaterialInstances?
    ) {
        // TODO block state
        // 带有模型的方块情况：通过行为包定义模型和贴图
//        val model = when (geometry) {
//            is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
//            is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
//            null -> "${identifier.namespace}:block/${identifier.path}"
//        }
        val model = "${identifier.namespace}:block/${identifier.path}"
        val blockState = JavaBlockStatesDefinition(
                variants = mapOf(
                        "" to JavaBlockStatesDefinition.Variant(model)
                )
        )
        FileWriter(file).use { writer ->
            GsonUtil.GSON.toJson(blockState, writer)
        }
    }

    /**
     * 根据方块材质包中的定义，创建一个方块贴图文件（附带命名空间）
     */
    private fun createBlockTextures(
        identifier: Identifier,
        textures: BlockResourceDefinition.Textures?,
        materialInstances: ComponentMaterialInstances?
    ) {
        val namespaceDir = this.namespaceDir(identifier.namespace)
        when (textures) {
            is BlockResourceDefinition.Textures.TexturesAllFace -> {
                val texture = context.resource.terrainTexture[textures.all]?.textures
                if (texture == null || !texture.contains("textures/")) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                    return
                }
                val bedrockTexture = context.resource.textureImages[texture]
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
                        val texture = context.resource.terrainTexture[it]?.textures
                        if (texture == null || !texture.contains("textures/")) {
                            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $it")
                            return
                        }
                        val bedrockTexture = context.resource.textureImages[texture]
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
            else -> {}
        }
        materialInstances?.forEach { name, instance ->
            val texture = context.resource.terrainTexture[instance.texture]?.textures
            if (texture == null || !texture.contains("textures/")) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Material instance texture not found: ${instance.texture}")
                return@forEach
            }
            val bedrockTexture = context.resource.textureImages[texture]
            if (bedrockTexture == null) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Material instance texture not found: ${instance.texture}")
                return@forEach
            }
            val file = namespaceDir.resolve(texture + "." + bedrockTexture.type.getExtension())
            bedrockTexture.image.let { image ->
                ImageIO.write(image, file.extension, file)
            }

        }
    }

    /**
     * 根据方块材质包和行为包中的定义，创建一个方块模型文件（如果设定了模型，则应用模型；否则应用正常方块模型）
     */
    private fun createBlockModel(file: File, identifier: Identifier, textures: BlockResourceDefinition.Textures?, geometry: ComponentGeometry?, materialInstances: ComponentMaterialInstances?) {
        if (geometry != null) {
            // 带有模型的方块情况：通过行为包定义模型和贴图
            val geometryIdentifier = when (geometry) {
                is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
                is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
            }
            val bedrockModel = BedrockAddonsRegistry.models[geometryIdentifier] ?: return
            val textureKey = materialInstances?.get("*")?.texture ?: return
            val terrainTexture = context.resource.terrainTexture[textureKey]?.textures ?: return
            val texture = if (terrainTexture.startsWith("textures/", ignoreCase = true)) terrainTexture.substring("textures/".length) else terrainTexture
            val spriteId = SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier(identifier.namespace, texture))
            bedrockModel.addSprite(spriteId)
        } else {
            val model = JavaModelDefinition()
            model.parent = "block/cube_all"

            // 通过行为包定义了贴图
            materialInstances?.forEach { (key, value) ->
                if (key == "*") {
                    value.texture?.let { texture ->
                        val javaTexture = context.resource.terrainTextureToJava(texture, identifier.namespace)
                        if (javaTexture != null) {
                            model.textures = mapOf("all" to javaTexture)  // TODO 不确定是什么key...
                        }
                    }
                } else {
                    BedrockLoader.logger.info("[BedrockResourcePackLoader] Material instance $key -> $value is not supported yet.")
                }
            }

            // 普通方块情况：通过材质包的blocks.json定义贴图
            when (textures) {
                is BlockResourceDefinition.Textures.TexturesAllFace -> {
                    val texture = context.resource.terrainTexture[textures.all]?.textures
                    if (texture == null || !texture.contains("textures/")) {
                        BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                        return
                    }
                    context.resource.terrainTextureToJava(textures.all, identifier.namespace)
                        ?.let { model.textures = mapOf("all" to it) }
                }

                is BlockResourceDefinition.Textures.TexturesMultiFace -> {
                    val texturesMap = mutableMapOf<String, Identifier>()

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
                            val texture = context.resource.terrainTextureToJava(it, identifier.namespace)
                            if (texture != null) {
                                texturesMap[direction] = texture
                            }
                        }
                    }

                    model.textures = texturesMap
                }

                else -> {}
            }

            FileWriter(file).use { writer ->
                GsonUtil.GSON.toJson(model, writer)
            }
        }
    }

    /**
     * 直接创建一个继承于对应方块模型的物品模型
     */
    private fun createItemModel(file: File, identifier: Identifier) {
        val model = JavaModelDefinition(
                parent = Identifier(identifier.namespace, "block/${identifier.path}").toString()
        )
        FileWriter(file).use { writer ->
            GsonUtil.GSON.toJson(model, writer)
        }
    }

    /**
     * 从ClientEntity读取需要的贴图，然后将对应的贴图文件保存到java材质包中（对应命名空间）
     */
    private fun createEntityTextures(identifier: Identifier, clientEntity: EntityResourceDefinition.ClientEntityDescription) {
        val namespaceDir = this.namespaceDir(identifier.namespace)
        clientEntity.textures?.forEach { (_, texture) ->
            val bedrockTexture = context.resource.textureImages[texture]
            if (bedrockTexture == null) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity texture not found: $texture")
                return
            }
            val file = namespaceDir.resolve(texture + "." + bedrockTexture.type.getExtension())
            file.parentFile.mkdirs()
            bedrockTexture.image.let { image ->
                ImageIO.write(image, file.extension, file)
            }
        }
    }

    /**
     * 从ClientEntity读取需要的渲染控制器，然后注册到Java版的渲染控制器中
     */
    private fun registerRenderController(clientEntity: EntityResourceDefinition.ClientEntityDescription, identifier: Identifier, entityType: EntityType<EntityDataDriven>) {
        clientEntity.render_controllers?.let { controllers ->
            if (controllers.isNotEmpty()) {
                if (controllers.size > 1) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity $identifier has more than one render controller, only the first one will be used.")
                }
                val controller = controllers[0]
                val renderController = context.resource.renderControllers[controller]
                if (renderController != null) {
                    val renderControllerInstance = RenderControllerWithClientEntity(renderController, clientEntity)
                    EntityRendererRegistry.register(entityType) { context -> EntityDataDrivenRenderer.create(context, renderControllerInstance, 0.5f) }
                    BedrockLoader.logger.debug("[BedrockResourcePackLoader] Entity {} render controller registered: {}", identifier, controller)
                } else {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity $identifier render controller not found: $controller")
                }
            }
        }
    }

}