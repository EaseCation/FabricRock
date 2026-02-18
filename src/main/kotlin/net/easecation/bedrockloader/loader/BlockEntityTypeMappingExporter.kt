package net.easecation.bedrockloader.loader

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.easecation.bedrockloader.BedrockLoader
import net.minecraft.registry.Registries
import java.io.File

object BlockEntityTypeMappingExporter {
    /**
     * 导出所有自定义方块实体的类型 ID 映射到 JSON 文件
     * 格式兼容 ViaBedrock 的 custom_block_entity_type_ids.json
     */
    fun export() {
        try {
            val mappings = JsonObject()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

            // 遍历所有注册的方块实体
            BedrockAddonsRegistry.blockEntities.forEach { (identifier, blockEntityType) ->
                val rawId = Registries.BLOCK_ENTITY_TYPE.getRawId(blockEntityType)

                val entry = JsonObject()
                entry.addProperty("java_type_id", rawId)
                entry.addProperty("block_identifier", identifier.toString())
                mappings.add(identifier.toString(), entry)
            }

            // 构建完整的 JSON 结构
            val root = JsonObject()
            root.add("mappings", mappings)

            // 写入文件
            val outputDir = File(BedrockLoader.getGameDir(), "config/bedrock-loader")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "custom_block_entity_type_ids.json")
            outputFile.writeText(gson.toJson(root))

            BedrockLoader.logger.info("Exported block entity type mappings to: ${outputFile.absolutePath}")
            BedrockLoader.logger.info("Exported ${mappings.size()} block entity type mappings")
        } catch (e: Exception) {
            BedrockLoader.logger.error("Failed to export block entity type mappings", e)
        }
    }
}
