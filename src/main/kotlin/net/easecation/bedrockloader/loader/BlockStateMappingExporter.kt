package net.easecation.bedrockloader.loader

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.easecation.bedrockloader.BedrockLoader
import net.easecation.bedrockloader.bedrock.block.component.ComponentCollisionBox
import net.easecation.bedrockloader.bedrock.block.component.FaceDirectionalType
import net.easecation.bedrockloader.block.BlockContext
import net.easecation.bedrockloader.block.property.BedrockBooleanProperty
import net.easecation.bedrockloader.block.property.BedrockProperty
import net.minecraft.block.Block
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage
import java.io.File

object BlockStateMappingExporter {

    //? if >=1.21.4 {
    // Java direction 约定 (N=0,S=1,W=2,E=3) → Bedrock 约定 (S=0,W=1,N=2,E=3)
    private val JAVA_TO_BEDROCK_DIRECTION = intArrayOf(2, 0, 1, 3)
    //?}

    /**
     * 颜色-原版方块映射表
     * 约 25 个代表性原版方块及其 RGB 平均色，用于根据纹理颜色匹配最接近的原版方块
     */
    private val COLOR_TO_BLOCK_TABLE: List<Pair<Triple<Int, Int, Int>, String>> = listOf(
        Triple(128, 128, 128) to "minecraft:stone",
        Triple(136, 136, 136) to "minecraft:cobblestone",
        Triple(216, 201, 159) to "minecraft:sandstone",
        Triple(181, 97, 31) to "minecraft:red_sandstone",
        Triple(134, 96, 67) to "minecraft:dirt",
        Triple(127, 178, 56) to "minecraft:grass_block",
        Triple(162, 130, 78) to "minecraft:oak_planks",
        Triple(115, 85, 49) to "minecraft:spruce_planks",
        Triple(195, 177, 121) to "minecraft:birch_planks",
        Triple(66, 43, 20) to "minecraft:dark_oak_planks",
        Triple(233, 236, 236) to "minecraft:white_wool",
        Triple(21, 21, 26) to "minecraft:black_wool",
        Triple(160, 39, 34) to "minecraft:red_wool",
        Triple(53, 57, 157) to "minecraft:blue_wool",
        Triple(84, 109, 27) to "minecraft:green_wool",
        Triple(248, 198, 39) to "minecraft:yellow_wool",
        Triple(240, 118, 19) to "minecraft:orange_wool",
        Triple(121, 42, 173) to "minecraft:purple_wool",
        Triple(220, 220, 220) to "minecraft:iron_block",
        Triple(246, 208, 61) to "minecraft:gold_block",
        Triple(98, 219, 214) to "minecraft:diamond_block",
        Triple(133, 97, 168) to "minecraft:amethyst_block",
        Triple(80, 80, 82) to "minecraft:deepslate",
        Triple(97, 38, 38) to "minecraft:netherrack",
        Triple(219, 222, 158) to "minecraft:end_stone",
    )

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

            // 预计算每个方块的 fallback（per-identifier，同一方块所有 state 共享）
            val fallbackCache = mutableMapOf<Identifier, String>()

            // 遍历所有注册的方块
            BedrockAddonsRegistry.blockContexts.forEach { (identifier, context) ->
                val block = BedrockAddonsRegistry.blocks[identifier] ?: return@forEach

                // 检测方块是否使用 netease:face_directional 的 direction 类型
                val hasFaceDirectionalDirection =
                    context.behaviour.components.neteaseFaceDirectional?.type == FaceDirectionalType.direction

                // 计算 fallback
                val fallbackBlock = fallbackCache.getOrPut(identifier) {
                    computeFallbackBlock(identifier, context)
                }

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
                        val bedrockValue = when {
                            // netease:face_directional direction 属性需要从 Java 约定转换为 Bedrock 约定
                            hasFaceDirectionalDirection && bedrockName == "direction" -> {
                                //? if >=1.21.4 {
                                JAVA_TO_BEDROCK_DIRECTION[javaValue.toInt()].toString()
                                //?} else {
                                /*toBedrockValue(prop, javaValue)  // pre-1.21.4 已经是 Bedrock 约定
                                *///?}
                            }
                            else -> toBedrockValue(prop, javaValue)
                        }
                        bedrockProps[bedrockName] = bedrockValue
                        javaProps[bedrockName] = javaValue
                    }

                    // 生成方块状态字符串
                    val bedrockStateStr = formatBlockState(identifier.toString(), bedrockProps)
                    val javaStateStr = formatBlockState(identifier.toString(), javaProps)
                    val javaStateId = Block.getRawIdFromState(state)

                    // 计算光照属性
                    val lightEmission = context.behaviour.components.minecraftLightEmission ?: 0
                    val defaultDampening = if (context.behaviour.components.neteaseBlockEntity != null) 0 else 15
                    val lightFilter = context.behaviour.components.minecraftLightDampening ?: defaultDampening

                    // 添加到映射表
                    val mapping = JsonObject()
                    mapping.addProperty("java_state", javaStateStr)
                    mapping.addProperty("java_state_id", javaStateId)
                    mapping.addProperty("light_emission", lightEmission)
                    mapping.addProperty("light_filter", lightFilter)
                    mapping.addProperty("fallback_block", fallbackBlock)
                    mappings.add(bedrockStateStr, mapping)
                }
            }

            // 构建完整的 JSON 结构
            val root = JsonObject()
            root.addProperty("format_version", 2)
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
     * 为指定方块计算 fallback 原版方块
     *
     * 逻辑顺序：
     * 1. 检查碰撞箱：无碰撞或非常矮 → 空气
     * 2. 检查 map_color 组件：有显式颜色 → 匹配最近原版方块
     * 3. 分析纹理平均颜色 → 匹配最近原版方块
     * 4. 默认 → 石头
     */
    private fun computeFallbackBlock(identifier: Identifier, context: BlockContext): String {
        val components = context.behaviour.components

        // 1. 检查碰撞箱
        when (val collisionBox = components.minecraftCollisionBox) {
            is ComponentCollisionBox.ComponentCollisionBoxBoolean -> {
                if (!collisionBox.value) return "minecraft:air"
            }
            is ComponentCollisionBox.ComponentCollisionBoxCustom -> {
                // size[1] 是高度 (Y 方向)，Bedrock 像素单位
                if (collisionBox.size[1] < 3.0f) return "minecraft:air"
            }
            null -> {} // 没有碰撞箱组件，按有碰撞处理
        }

        // 2. 优先使用 map_color（方块作者显式指定的颜色）
        val mapColor = components.minecraftMapColor
        if (mapColor != null) {
            val color = mapColor.getColor()
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            return findClosestBlockByColor(r, g, b)
        }

        // 3. 尝试通过纹理获取颜色
        val materialInstances = components.minecraftMaterialInstances
        if (materialInstances != null) {
            // 按优先级尝试: "*" (默认), "up", 第一个可用的
            val textureKey = materialInstances["*"]?.texture
                ?: materialInstances["up"]?.texture
                ?: materialInstances.values.firstOrNull()?.texture

            if (textureKey != null) {
                val avgColor = getTextureAverageColor(textureKey)
                if (avgColor != null) {
                    return findClosestBlockByColor(avgColor.first, avgColor.second, avgColor.third)
                }
            }
        }

        // 4. 默认回退到石头
        return "minecraft:stone"
    }

    /**
     * 通过纹理键获取纹理的平均颜色
     * 纹理键 → terrain_texture.json 查路径 → textureImages 取 BufferedImage → 计算均色
     */
    private fun getTextureAverageColor(textureKey: String): Triple<Int, Int, Int>? {
        val resourceContext = BedrockAddonsLoader.context.resource
        val textureDef = resourceContext.terrainTexture[textureKey]
        val texturePath = textureDef?.textures?.firstOrNull()?.path ?: return null
        val textureImage = resourceContext.textureImages[texturePath] ?: return null
        return computeAverageColor(textureImage.image)
    }

    /**
     * 计算 BufferedImage 中非透明像素的 RGB 平均色
     * @return RGB 三元组，全透明纹理返回 null
     */
    private fun computeAverageColor(image: BufferedImage): Triple<Int, Int, Int>? {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0L
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val alpha = (argb shr 24) and 0xFF
                if (alpha < 128) continue  // 跳过透明/半透明像素
                totalR += (argb shr 16) and 0xFF
                totalG += (argb shr 8) and 0xFF
                totalB += argb and 0xFF
                count++
            }
        }
        if (count == 0L) return null
        return Triple(
            (totalR / count).toInt(),
            (totalG / count).toInt(),
            (totalB / count).toInt()
        )
    }

    /**
     * 用欧几里得距离匹配颜色最接近的原版方块
     */
    private fun findClosestBlockByColor(r: Int, g: Int, b: Int): String {
        var minDist = Long.MAX_VALUE
        var closest = "minecraft:stone"
        for ((color, block) in COLOR_TO_BLOCK_TABLE) {
            val dr = (r - color.first).toLong()
            val dg = (g - color.second).toLong()
            val db = (b - color.third).toLong()
            val dist = dr * dr + dg * dg + db * db
            if (dist < minDist) {
                minDist = dist
                closest = block
            }
        }
        return closest
    }

    /**
     * 将 Java 属性值转换为 Bedrock 格式
     * 唯一需要转换的是 Boolean: true/false -> 1/0
     */
    //? if >=1.21.2 {
    private fun toBedrockValue(property: BedrockProperty<*>, javaValue: String): String {
    //?} else {
    /*private fun toBedrockValue(property: BedrockProperty<*, *>, javaValue: String): String {
    *///?}
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
