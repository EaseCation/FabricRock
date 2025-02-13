package net.easecation.bedrockloader.loader.deserializer

import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.data.TextureImage
import net.easecation.bedrockloader.bedrock.definition.*
import net.easecation.bedrockloader.loader.context.BedrockResourceContext
import net.easecation.bedrockloader.util.GsonUtil
import java.io.InputStreamReader
import java.util.zip.ZipFile
import javax.imageio.ImageIO

object BedrockResourceDeserializer : PackDeserializer<BedrockResourceContext> {

    override fun deserialize(file: ZipFile): BedrockResourceContext {
        val context = BedrockResourceContext()
        file.use { zip ->
            zip.getEntry("textures/terrain_texture.json")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val terrainTextureDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), TerrainTextureDefinition::class.java)
                    context.terrainTexture.putAll(terrainTextureDefinition.texture_data)
                }
            }
            zip.getEntry("textures/item_texture.json")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val itemTextureDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), ItemTextureDefinition::class.java)
                    context.itemTexture.putAll(itemTextureDefinition.texture_data)
                }
            }
            zip.getEntry("blocks.json")?.let { entry ->
                zip.getInputStream(entry).use { stream ->
                    val blockResourceDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), BlockResourceDefinition::class.java)
                    context.blocks.putAll(blockResourceDefinition.blocks)
                }
            }
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                // texture
                if (name.startsWith("textures/") && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".tga"))) {
                    try {
                        val ext = name.substring(name.lastIndexOf('.') + 1)
                        val withoutExt = name.substring(0, name.lastIndexOf('.'))
                        context.textureImages[withoutExt] = TextureImage(ImageIO.read(zip.getInputStream(entry)), ext)
                    } catch (e: Exception) {
                        BedrockLoader.logger.error("Error parsing texture: $name", e)
                    }
                }
                // geometry
                if (name.endsWith(".geo.json")) {
                    zip.getInputStream(entry).use { stream ->
                        try {
                            val geometryDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), GeometryDefinition::class.java)
                            for (model in geometryDefinition.geometry) {
                                context.geometries[model.description.identifier] = model
                            }
                        } catch (e: Exception) {
                            BedrockLoader.logger.error("Error parsing geometry: $name", e)
                        }
                    }
                }
                // entity
                if (name.endsWith(".entity.json")) {
                    zip.getInputStream(entry).use { stream ->
                        try {
                            val entityResourceDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityResourceDefinition::class.java)
                            context.entities.put(entityResourceDefinition.clientEntity.description.identifier, entityResourceDefinition.clientEntity)
                        } catch (e: Exception) {
                            BedrockLoader.logger.error("Error parsing client entity: $name", e)
                        }
                    }
                }
                // render controller
                if (name.endsWith(".render_controllers.json") || name.endsWith(".render_controller.json")) {
                    zip.getInputStream(entry).use { stream ->
                        try {
                            val renderControllerDefinition = GsonUtil.GSON.fromJson(InputStreamReader(stream), EntityRenderControllerDefinition::class.java)
                            context.renderControllers.putAll(renderControllerDefinition.renderControllers)
                        } catch (e: Exception) {
                            BedrockLoader.logger.error("Error parsing render controller: $name", e)
                        }
                    }
                }
            }
        }
        return context
    }

}