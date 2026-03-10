package net.easecation.bedrockloader.loader

import net.easecation.bedrockloader.bedrock.block.component.ComponentMaterialInstances
import net.easecation.bedrockloader.bedrock.definition.AnimationDefinition
import net.easecation.bedrockloader.loader.context.BedrockPackContext
import net.easecation.bedrockloader.render.BedrockEntityMaterial
import net.easecation.bedrockloader.render.BedrockGeometryModel
import net.easecation.bedrockloader.render.BedrockRenderUtil
//? if <1.21.4 {
/*import net.minecraft.client.render.model.UnbakedModel
*///?}
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier

/**
 * 实体动画配置，用于懒加载创建 EntityAnimationManager
 */
data class EntityAnimationConfig(
    val animationMap: Map<String, String>,                    // 别名 -> 动画ID
    val animations: Map<String, AnimationDefinition.Animation>, // 动画ID -> 动画数据
    val autoPlayList: List<String>                            // scripts.animate 列表
)

/**
 * Multiblock 控制方块的合并物品模型数据
 * parts: 每个 part 的 BedrockGeometryModel 及其相对偏移量 [x, y, z]（block 坐标）
 */
data class MultiblockItemData(
    val parts: List<Pair<BedrockGeometryModel, List<Int>>>
)

object BedrockAddonsRegistryClient {
    val geometries: MutableMap<String, BedrockGeometryModel.Factory> = mutableMapOf()
    //? if >=1.21.4 {
    val blockModels: MutableMap<Identifier, Any> = mutableMapOf()
    //?} else {
    /*val blockModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    *///?}
    val blockEntityModels: MutableMap<Identifier, BedrockGeometryModel> = mutableMapOf()
    //? if >=1.21.4 {
    val itemModels: MutableMap<Identifier, Any> = mutableMapOf()
    //?} else {
    /*val itemModels: MutableMap<Identifier, UnbakedModel> = mutableMapOf()
    *///?}
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

    /**
     * Multiblock 控制方块的合并物品模型数据
     * key: 控制方块的 Identifier，value: 所有 part 的模型和偏移量
     */
    val multiblockItemData: MutableMap<Identifier, MultiblockItemData> = mutableMapOf()

    private val multiblockMeshCache: MutableMap<Identifier, Mesh> = mutableMapOf()

    /**
     * 清除 multiblock 物品模型缓存（在资源包重载时调用）
     */
    fun clearMultiblockMeshCache() {
        multiblockMeshCache.clear()
    }

    /**
     * 获取或构建 multiblock 控制方块的合并 Mesh。
     * 首次调用时从各 part 的已烘焙 mesh 合并，之后缓存复用。
     * 若 part 模型尚未烘焙（block entity 类型），则从纹理 atlas 现场取 sprite 并烘焙。
     */
    fun getOrBuildMultiblockMesh(id: Identifier, data: MultiblockItemData): Mesh? {
        multiblockMeshCache[id]?.let { return it }
        //? if <1.21.5 {
        /*val mc = MinecraftClient.getInstance()
        *///?}
        val meshesWithOffsets = data.parts.mapNotNull { (model, offset) ->
            val existingMesh = model.getBakedMesh()
            if (existingMesh != null) return@mapNotNull Pair(existingMesh, offset)
            //? if <1.21.5 {
            /*val sprites = model.materials.mapValues { (_, mat): Map.Entry<String, net.easecation.bedrockloader.render.BedrockMaterialInstance> ->
                mc.bakedModelManager.getAtlas(mat.spriteId.atlasId).getSprite(mat.spriteId.textureId)
            }
            val defaultSprite = sprites["*"] ?: sprites.values.firstOrNull() ?: return@mapNotNull null
            val doubleSidedMaterials = model.materials.entries
                .filter { (_, mat) -> mat.renderMethod == ComponentMaterialInstances.RenderMethod.alpha_test
                                   || mat.renderMethod == ComponentMaterialInstances.RenderMethod.double_sided }
                .map { it.key }.toSet()
            val mesh = BedrockRenderUtil.bakeModelPartToMesh(model.getModelPartForBaking(), defaultSprite, sprites, model.getBlockTransformation(), doubleSidedMaterials)
            Pair(mesh, offset)
            *///?} else {
            null
            //?}
        }
        if (meshesWithOffsets.isEmpty()) return null
        val combined = BedrockRenderUtil.combineMultiblockMeshes(meshesWithOffsets)
        multiblockMeshCache[id] = combined
        return combined
    }
}
