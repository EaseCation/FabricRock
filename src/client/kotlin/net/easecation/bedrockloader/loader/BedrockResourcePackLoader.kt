package net.easecation.bedrockloader.loader

import com.mojang.datafixers.util.Either
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.component.ComponentGeometry
import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.bedrock.definition.BlockResourceDefinition
import net.easecation.bedrockloader.bedrock.definition.EntityResourceDefinition
import net.easecation.bedrockloader.entity.EntityDataDriven
import net.easecation.bedrockloader.java.definition.JavaMCMeta
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.easecation.bedrockloader.render.BedrockMaterialInstance
import net.easecation.bedrockloader.render.renderer.EntityDataDrivenRenderer
import net.easecation.bedrockloader.util.GsonUtil
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.entity.EntityType
import net.minecraft.screen.PlayerScreenHandler
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
        this.init()
        // Geometry
        context.resource.geometries.forEach { (key, value) ->
            BedrockAddonsRegistryClient.geometries[key] = BedrockGeometryModel.Factory(value)
        }
        // Blocks
        for ((identifier, block) in context.resource.blocks) {
            val blockComponents = context.behavior.blocks[identifier]?.components
            // textures
            createBlockTextures(identifier, block, blockComponents)
            // models
            createBlockModel(identifier, block, blockComponents)
            createBlockItemModel(identifier, block, blockComponents)
            // renderer
            registerBlockRenderLayer(identifier, blockComponents)
        }
        // Entity
        for ((identifier, entity) in context.resource.entities) {
            val clientEntity = entity.description
            val entityType = BedrockAddonsRegistry.entities[identifier]
            // textures
            createEntityTextures(identifier, clientEntity)
            createSpawnEggTextures(identifier, clientEntity)
            // models
            createSpawnEggModel(identifier, clientEntity)
            // renderer
            registerRenderController(identifier, clientEntity, entityType)
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
            val textures = namespaceDir.resolve("textures")
            if (!textures.exists()) {
                textures.mkdirs()
            }
            val texturesItem = textures.resolve("item")
            if (!texturesItem.exists()) {
                texturesItem.mkdirs()
            }
            val texturesBlock = textures.resolve("block")
            if (!texturesBlock.exists()) {
                texturesBlock.mkdirs()
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
     * 根据方块材质包中的定义，创建一个方块贴图文件（附带命名空间）
     */
    private fun createBlockTextures(
        identifier: Identifier,
        block: BlockResourceDefinition.Block,
        blockComponents: BlockComponents?
    ) {
        block.textures?.let { createTextures(identifier, it) }
        block.carried_textures?.let { createTextures(identifier, it) }
        blockComponents?.minecraftMaterialInstances?.values?.forEach { instance ->
            instance.texture?.let { createBlockTexture(identifier, it) }
        }
    }

    private fun createTextures(
        identifier: Identifier,
        textures: BlockResourceDefinition.Textures
    ) {
        when (textures) {
            is BlockResourceDefinition.Textures.TexturesAllFace -> {
                createBlockTexture(identifier, textures.all)
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
                    textureKey?.let { createBlockTexture(identifier, it) }
                }
            }
        }
    }

    /**
     * 根据方块材质包中的定义，创建一个方块贴图文件（附带命名空间）
     */
    private fun createBlockTexture(
        identifier: Identifier,
        textureKey: String
    ) {
        val namespaceDir = this.namespaceDir(identifier.namespace)
        val texture = context.resource.terrainTexture[textureKey]?.textures
        if (texture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $textureKey")
            return
        }
        val path = "textures/block/${texture.substringAfterLast("/")}"
        val bedrockTexture = context.resource.textureImages[texture]
        if (bedrockTexture == null) {
            BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: $textureKey")
            return
        }
        val file = namespaceDir.resolve(path + "." + bedrockTexture.type.getExtension())
        bedrockTexture.image.let { image ->
            ImageIO.write(image, file.extension, file)
        }
    }

    /**
     * 根据方块材质包和行为包中的定义，创建一个方块模型文件（如果设定了模型，则应用模型；否则应用正常方块模型）
     */
    private fun createBlockModel(
        identifier: Identifier,
        block: BlockResourceDefinition.Block,
        blockComponents: BlockComponents?
    ) {
        val geometry = blockComponents?.minecraftGeometry
        val materialInstances = blockComponents?.minecraftMaterialInstances
        if (geometry != null) {
            // 带有模型的方块情况：通过行为包定义模型和贴图
            val model = createGeometryModel(identifier, geometry, materialInstances) ?: return
            BedrockAddonsRegistryClient.blockModels[identifier] = model
        } else {
            val textures = block.textures
            val model = createCubeModel(identifier, textures, materialInstances)
            BedrockAddonsRegistryClient.blockModels[identifier] = model
        }
    }

    /**
     * 直接创建一个继承于对应方块模型的物品模型
     */
    private fun createBlockItemModel(
        identifier: Identifier,
        block: BlockResourceDefinition.Block,
        blockComponents: BlockComponents?
    ) {
        val geometry = blockComponents?.minecraftGeometry
        val materialInstances = blockComponents?.minecraftMaterialInstances
        if (geometry != null) {
            // 带有模型的方块情况：通过行为包定义模型和贴图
            val model = createGeometryModel(identifier, geometry, materialInstances) ?: return
            BedrockAddonsRegistryClient.itemModels[identifier] = model
        } else {
            val textures = block.carried_textures ?: block.textures
            val model = createCubeModel(identifier, textures, materialInstances)
            BedrockAddonsRegistryClient.itemModels[identifier] = model
        }
    }

    private fun createCubeModel(
        identifier: Identifier,
        textures: BlockResourceDefinition.Textures?,
        materialInstances: ComponentMaterialInstances?
    ): JsonUnbakedModel {
        val textureMap = mutableMapOf<String, Either<SpriteIdentifier, String>>()
        // 通过行为包定义了贴图
        materialInstances?.forEach { (key, value) ->
            if (key == "*") {
                value.texture?.let { texture ->
                    context.resource.terrainTextureToJava(identifier.namespace, texture)?.let {
                        textureMap["all"] = Either.left(SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, it))
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
                if (texture == null) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block texture not found: ${textures.all}")
                } else {
                    context.resource.terrainTextureToJava(identifier.namespace, textures.all)?.let {
                        textureMap["all"] = Either.left(SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, it))
                    }
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
                for ((direction, textureKey) in directions) {
                    textureKey?.let {
                        context.resource.terrainTextureToJava(identifier.namespace, it)?.let { texture ->
                            textureMap[direction] = Either.left(SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, texture))
                        }
                    }
                }
            }
            else -> BedrockLoader.logger.warn("[BedrockResourcePackLoader] Block $identifier has no textures defined.")
        }
        return JsonUnbakedModel(Identifier("block/cube_all"), emptyList(), textureMap, null, null, ModelTransformation.NONE, emptyList())
    }

    private fun createGeometryModel(
        identifier: Identifier,
        geometry: ComponentGeometry,
        materialInstances: ComponentMaterialInstances?
    ): BedrockGeometryModel? {
        val geometryIdentifier = when (geometry) {
            is ComponentGeometry.ComponentGeometrySimple -> geometry.identifier
            is ComponentGeometry.ComponentGeometryFull -> geometry.identifier
        }
        val geometryFactory = BedrockAddonsRegistryClient.geometries[geometryIdentifier]
        val materials = materialInstances?.mapNotNull { (key, material) ->
            val textureKey = material.texture ?: return@mapNotNull null
            val texture = context.resource.terrainTexture[textureKey]?.textures ?: return@mapNotNull null
            val spriteId = SpriteIdentifier(
                PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier(identifier.namespace, "block/${texture.substringAfterLast("/")}")
            )
            return@mapNotNull key to BedrockMaterialInstance(spriteId)
        }?.toMap() ?: emptyMap()
        return geometryFactory?.create(materials)
    }

    private fun registerBlockRenderLayer(
        identifier: Identifier,
        blockComponents: BlockComponents?
    ) {
        val materialInstances = blockComponents?.minecraftMaterialInstances
        val block = BedrockAddonsRegistry.blocks[identifier] ?: return
        val renderMethod = materialInstances?.get("*")?.render_method ?: return
        if (renderMethod == ComponentMaterialInstances.RenderMethod.alpha_test) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout())
        } else if (renderMethod == ComponentMaterialInstances.RenderMethod.blend) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent())
        }
    }

    private fun createSpawnEggTextures(
        identifier: Identifier,
        clientEntity: EntityResourceDefinition.ClientEntityDescription
    ) {
        val namespaceDir = this.namespaceDir(identifier.namespace)
        val spawnEggTexture = clientEntity.spawn_egg?.texture
        if (spawnEggTexture != null) {
            val texture = context.resource.itemTexture[spawnEggTexture]?.textures
            if (texture == null) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity spawn egg texture not found: $spawnEggTexture")
                return
            }
            val path = "textures/item/${texture.substringAfterLast("/")}"
            val bedrockTexture = context.resource.textureImages[texture]
            if (bedrockTexture == null) {
                BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity spawn egg texture not found: $spawnEggTexture")
                return
            }
            val file = namespaceDir.resolve(path + "." + bedrockTexture.type.getExtension())
            bedrockTexture.image.let { image ->
                ImageIO.write(image, file.extension, file)
            }
        }
    }

    /**
     * 直接创建一个继承于对应实体的生物蛋物品
     */
    private fun createSpawnEggModel(
        identifier: Identifier,
        clientEntity: EntityResourceDefinition.ClientEntityDescription
    ) {
        context.behavior.entities[identifier]?.description?.is_spawnable?.let {
            val entityName = identifier.path
            val itemIdentifier = Identifier(identifier.namespace, "${entityName}_spawn_egg")
            val spawnEggTexture = clientEntity.spawn_egg?.texture
            if (spawnEggTexture != null) {
                val textureMap = mutableMapOf<String, Either<SpriteIdentifier, String>>()
                context.resource.itemTextureToJava(itemIdentifier.namespace, spawnEggTexture)?.let {
                    textureMap["layer0"] = Either.left(SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, it))
                }
                BedrockAddonsRegistryClient.itemModels[itemIdentifier] = JsonUnbakedModel(Identifier("item/generated"), emptyList(), textureMap, null, null, ModelTransformation.NONE, emptyList())
            } else {
                BedrockAddonsRegistryClient.itemModels[itemIdentifier] = DelegatingUnbakedModel(Identifier("item/template_spawn_egg"))
            }
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
    private fun registerRenderController(
        identifier: Identifier,
        clientEntity: EntityResourceDefinition.ClientEntityDescription,
        entityType: EntityType<EntityDataDriven>?
    ) {
        if (entityType == null) return
        clientEntity.render_controllers?.let { controllers ->
            if (controllers.isNotEmpty()) {
                if (controllers.size > 1) {
                    BedrockLoader.logger.warn("[BedrockResourcePackLoader] Entity {} has more than one render controller, only the first one will be used.", clientEntity.identifier)
                }
                val controller = controllers[0]
                val renderController = context.resource.renderControllers[controller]
                val geometryMolang = renderController?.geometry ?: return@let
                val geometryAlias = geometryMolang.substringAfter("geometry.").substringAfter("Geometry.")
                val geometryIdentifier = clientEntity.geometry?.get(geometryAlias) ?: return@let
                val geometryFactory = BedrockAddonsRegistryClient.geometries[geometryIdentifier] ?: return@let
                val textureMolang = renderController.textures?.firstOrNull() ?: return@let
                val textureAlias = textureMolang.substringAfter("texture.").substringAfter("Texture.")
                val texture = clientEntity.textures?.get(textureAlias) ?: return@let
                val bedrockTexture = context.resource.textureImages[texture] ?: return@let
                val spriteId = SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier(identifier.namespace, texture + "." + bedrockTexture.type.getExtension()))
                val materials = mapOf("*" to BedrockMaterialInstance(spriteId))
                EntityRendererRegistry.register(entityType) { context ->
                    val model = geometryFactory.create(materials)
                    BedrockAddonsRegistryClient.entityModel[identifier] = model
                    EntityDataDrivenRenderer.create(context, model, 0.5f, spriteId.textureId)
                }
                BedrockLoader.logger.debug("[BedrockResourcePackLoader] Entity {} render controller registered: {}", clientEntity.identifier, controller)
            }
        }
    }

}