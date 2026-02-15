package net.easecation.bedrockloader.loader

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.easecation.bedrockloader.BedrockLoader
import net.minecraft.registry.Registries
import java.io.File

object EntityTypeMappingExporter {
    /**
     * 导出所有自定义实体的类型 ID 映射到 JSON 文件
     * 格式兼容 ViaBedrock 的 custom_entity_type_ids.json
     */
    fun export() {
        try {
            val mappings = JsonObject()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

            // 遍历所有注册的实体
            BedrockAddonsRegistry.entities.forEach { (identifier, entityType) ->
                val rawId = Registries.ENTITY_TYPE.getRawId(entityType)

                val entry = JsonObject()
                entry.addProperty("java_type_id", rawId)
                entry.addProperty("fallback_type", "minecraft:pig")
                mappings.add(identifier.toString(), entry)
            }

            // 构建完整的 JSON 结构
            val root = JsonObject()
            root.add("mappings", mappings)

            // 写入文件
            val outputDir = File(BedrockLoader.getGameDir(), "config/bedrock-loader")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "custom_entity_type_ids.json")
            outputFile.writeText(gson.toJson(root))

            BedrockLoader.logger.info("导出实体类型映射到: ${outputFile.absolutePath}")
            BedrockLoader.logger.info("共导出 ${mappings.size()} 个实体类型映射")
        } catch (e: Exception) {
            BedrockLoader.logger.error("导出实体类型映射失败", e)
        }
    }
}
