package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.easecation.bedrockloader.render.BedrockGeometryModel
//? if <1.21.4 {
import net.minecraft.client.render.model.UnbakedModel
//?}
import net.minecraft.util.Identifier

/**
 * 实体动画配置，用于懒加载创建 EntityAnimationManager
 */
data class EntityAnimationConfig(
    val animationMap: Map<String, String>,                    // 别名 -> 动画ID
    val animations: Map<String, AnimationDefinition.Animation>, // 动画ID -> 动画数据
    val autoPlayList: List<String>                            // scripts.animate 列表
)

object BedrockAddonsRegistryClient {
    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    //? if >=1.21.4 {
    /*val blockModels: MutableMap<Identifier, Any> = mutableMapOf()
    *///?} else {
    val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    //?}
    val blockEntityModels: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()
    //? if >=1.21.4 {
    /*val itemModels: MutableMap<Identifier, Any> = mutableMapOf()
    *///?} else {
    val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    //?}
    val entityModel: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()

    /**
     * 存储实体材质类型配置
     * key: 实体标识符, value: 材质类型
     */
    val entityMaterial: MutableMap<Identifier, BedrockEntityMaterial> = mutableMapOf()

    /**
     * 存储方块实体材质类型配置
     * key: 方块标识符, value: 材质类型
     */
    val blockEntityMaterial: MutableMap<Identifier, BedrockEntityMaterial> = mutableMapOf()

    /**
     * 存储每个namespace对应的资源包上下文，用于动态创建材质映射
     */
    val packContexts: MutableMap<String, BedrockPackContext> = mutableMapOf()

    /**
     * 存储实体动画配置，用于懒加载创建 EntityAnimationManager
     * key: 实体标识符
     */
    val entityAnimationConfigs: MutableMap<Identifier, EntityAnimationConfig> = mutableMapOf()

    /**
     * 存储方块实体动画配置，用于懒加载创建 EntityAnimationManager
     * key: 方块标识符
     */
    val blockEntityAnimationConfigs: MutableMap<Identifier, EntityAnimationConfig> = mutableMapOf()

    /**
     * 存储实体缩放配置
     * key: 实体标识符, value: 缩放值 (默认 1.0f)
     */
    val entityScaleConfigs: MutableMap<Identifier, Float> = mutableMapOf()

    /**
     * 存储方块实体缩放配置
     * key: 方块标识符, value: 缩放值 (默认 1.0f)
     */
    val blockEntityScaleConfigs: MutableMap<Identifier, Float> = mutableMapOf()
}