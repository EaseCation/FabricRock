package net.easecation.bedrockloader.loader

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.block.property.BedrockBooleanProperty
import net.easecation.bedrockloader.block.property.BedrockProperty
import net.minecraft.block.Block
import java.io.File

object BlockStateMappingExporter {
    /**
     * 导出所有自定义方块的状态映射到 JSON 文件
     * 格式：Bedrock 格式方块状态 -> Java 格式方块状态 + Java 状态 ID
     */
    fun export() {
        try {
            val mappings = JsonObject()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()  // 禁用 HTML 转义，保留 : 和 = 等字符
                .create()

            // 遍历所有注册的方块
            BedrockAddonsRegistry.blockContexts.forEach { (identifier, context) ->
                val block = BedrockAddonsRegistry.blocks[identifier] ?: return@forEach

                // 遍历该方块的所有状态
                block.stateManager.states.forEach { state ->
                    val bedrockProps = mutableMapOf<String, String>()
                    val javaProps = mutableMapOf<String, String>()

                    // 遍历所有属性
                    context.properties.forEach { (bedrockName, prop) ->
                        @Suppress("UNCHECKED_CAST")
                        val javaProperty = prop.javaProperty as net.minecraft.state.property.Property<Comparable<Any>>
                        val propertyValue = state.get(javaProperty)
                        val javaValue = javaProperty.name(propertyValue)
                        val bedrockValue = toBedrockValue(prop, javaValue)
                        bedrockProps[bedrockName] = bedrockValue
                        javaProps[bedrockName] = javaValue
                    }

                    // 生成方块状态字符串
                    val bedrockStateStr = formatBlockState(identifier.toString(), bedrockProps)
                    val javaStateStr = formatBlockState(identifier.toString(), javaProps)
                    val javaStateId = Block.getRawIdFromState(state)

                    // 添加到映射表
                    val mapping = JsonObject()
                    mapping.addProperty("java_state", javaStateStr)
                    mapping.addProperty("java_state_id", javaStateId)
                    mappings.add(bedrockStateStr, mapping)
                }
            }

            // 构建完整的 JSON 结构
            val root = JsonObject()
            root.addProperty("format_version", 1)
            root.add("mappings", mappings)

            // 写入文件
            val outputDir = File(BedrockLoader.getGameDir(), "config/bedrock-loader")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "block_state_mappings.json")
            outputFile.writeText(gson.toJson(root))

            BedrockLoader.logger.info("导出方块状态映射到: ${outputFile.absolutePath}")
            BedrockLoader.logger.info("共导出 ${mappings.size()} 个方块状态映射")
        } catch (e: Exception) {
            BedrockLoader.logger.error("导出方块状态映射失败", e)
        }
    }

    /**
     * 将 Java 属性值转换为 Bedrock 格式
     * 唯一需要转换的是 Boolean: true/false -> 1/0
     */
    //? if >=1.21.2 {
    /*private fun toBedrockValue(property: BedrockProperty<*>, javaValue: String): String {
    *///?} else {
    private fun toBedrockValue(property: BedrockProperty<*, *>, javaValue: String): String {
    //?}
        return when (property) {
            is BedrockBooleanProperty -> if (javaValue == "true") "1" else "0"
            else -> javaValue  // Int, String, Direction 都不需要转换
        }
    }

    /**
     * 格式化方块状态字符串
     * 格式: namespace:identifier[prop1=value1,prop2=value2]
     */
    private fun formatBlockState(identifier: String, properties: Map<String, String>): String {
        if (properties.isEmpty()) {
            return "$identifier[]"
        }
        val propsStr = properties.entries
            .sortedBy { it.key }  // 按属性名排序确保一致性
            .joinToString(",") { "${it.key}=${it.value}" }
        return "$identifier[$propsStr]"
    }
}
