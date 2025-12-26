package net.easecation.bedrockloader.loader.deserializer

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.*
import net.easecation.bedrockloader.loader.context.BedrockResourceContext
import net.easecation.bedrockloader.util.GsonUtil
import net.easecation.bedrockloader.util.TargaReader
import java.io.InputStreamReader
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object BedrockResourceDeserializer : PackDeserializer<BedrockResourceContext> {

    override fun deserialize(file: ZipFile): BedrockResourceContext {
        return deserialize(file, "")
    }

    override fun deserialize(file: ZipFile, pathPrefix: String): BedrockResourceContext {
        val context = BedrockResourceContext()

        // 辅助函数：获取带前缀的entry
        fun getEntryWithPrefix(path: String) = file.getEntry("$pathPrefix$path")

        // 辅助函数：检查相对路径是否匹配
        fun getRelativeName(name: String): String? {
            if (pathPrefix.isNotEmpty() && !name.startsWith(pathPrefix)) {
                return null
            }
            return if (pathPrefix.isNotEmpty()) name.removePrefix(pathPrefix) else name
        }

        // 读取terrain_texture.json
        getEntryWithPrefix("textures/terrain_texture.json")?.let { entry ->
            file.getInputStream(entry).use { stream ->
                val terrainTextureDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), TerrainTextureDefinition::class.java)
                context.terrainTexture.putAll(terrainTextureDefinition.texture_data)
            }
        }

        // 读取item_texture.json
        getEntryWithPrefix("textures/item_texture.json")?.let { entry ->
            file.getInputStream(entry).use { stream ->
                val itemTextureDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), ItemTextureDefinition::class.java)
                context.itemTexture.putAll(itemTextureDefinition.texture_data)
            }
        }

        // 读取blocks.json
        getEntryWithPrefix("blocks.json")?.let { entry ->
            file.getInputStream(entry).use { stream ->
                val blockResourceDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockResourceDefinition::class.java)
                context.blocks.putAll(blockResourceDefinition.blocks)
            }
        }

        // 遍历所有entries
        val entries = file.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name

            // 获取相对路径
            val relativeName = getRelativeName(name) ?: continue

            // texture
            if (relativeName.startsWith("textures/") && (relativeName.endsWith(".png") || relativeName.endsWith(".jpg") || relativeName.endsWith(".tga"))) {
                try {
                    val ext = relativeName.substring(relativeName.lastIndexOf('.') + 1)
                    val withoutExt = relativeName.substring(0, relativeName.lastIndexOf('.'))
                    val image = when(ext.lowercase()) {
                        "tga" -> TargaReader.read(file.getInputStream(entry))
                        else -> ImageIO.read(file.getInputStream(entry))
                    }
                    context.textureImages[withoutExt] = TextureImage(image, ext)
                } catch (e: Exception) {
                    BedrockLoader.logger.error("Error parsing texture: $name", e)
                }
            }

            // geometry
            // 支持 .geo.json 和 models/ 目录下的标准 .json 文件
            if (relativeName.endsWith(".geo.json") || (relativeName.startsWith("models/") && relativeName.endsWith(".json"))) {
                file.getInputStream(entry).use { stream ->
                    try {
                        val geometryDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), GeometryDefinition::class.java)
                        if (geometryDefinition.geometry == null) {
                            BedrockLoader.logger.warn("Skipping unsupported geometry format (possibly 1.8 legacy format): $name")
                            return@use
                        }
                        for (model in geometryDefinition.geometry) {
                            context.geometries[model.description.identifier] = model
                        }
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("Error parsing geometry: $name", e)
                    }
                }
            }

            // entity
            if (relativeName.endsWith(".entity.json")) {
                file.getInputStream(entry).use { stream ->
                    try {
                        val entityResourceDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityResourceDefinition::class.java)
                        context.entities.put(entityResourceDefinition.clientEntity.description.identifier, entityResourceDefinition.clientEntity)
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("Error parsing client entity: $name", e)
                    }
                }
            }

            // render controller
            if (relativeName.endsWith(".render_controllers.json") || relativeName.endsWith(".render_controller.json")) {
                file.getInputStream(entry).use { stream ->
                    try {
                        val renderControllerDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityRenderControllerDefinition::class.java)
                        context.renderControllers.putAll(renderControllerDefinition.renderControllers)
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("Error parsing render controller: $name", e)
                    }
                }
            }

            // animation
            // 支持 .animation.json 和 animations/ 目录下的标准 .json 文件
            if (relativeName.endsWith(".animation.json") || (relativeName.startsWith("animations/") && relativeName.endsWith(".json"))) {
                file.getInputStream(entry).use { stream ->
                    try {
                        val animationDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), AnimationDefinition::class.java)
                        context.animations.putAll(animationDefinition.animations)
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("Error parsing animation: $name", e)
                    }
                }
            }
        }

        return context
    }

}
