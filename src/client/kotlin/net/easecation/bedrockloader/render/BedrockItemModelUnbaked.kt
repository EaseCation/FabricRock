package net.easecation.bedrockloader.render

//? if >=1.21.5 {
import com.mojang.serialization.MapCodec
//? if <1.21.11 {
/*import net.minecraft.client.render.item.model.BasicItemModel
*///?}
import net.minecraft.client.render.item.model.ItemModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.ModelSettings
import net.minecraft.client.render.model.ResolvableModel
import net.minecraft.client.render.model.SimpleModel
import net.minecraft.client.texture.Sprite
import net.easecation.bedrockloader.util.identifierOf
import net.minecraft.util.Identifier
//? if >=1.21.11 {
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.item.ItemRenderState
import net.minecraft.client.item.ItemModelManager
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.HeldItemContext
//?}

class BedrockItemModelUnbaked(
    private val geometryModel: BedrockGeometryModel
) : ItemModel.Unbaked {

    override fun getCodec(): MapCodec<out ItemModel.Unbaked> {
        throw UnsupportedOperationException("BedrockItemModelUnbaked is not serializable")
    }

    override fun resolve(resolver: ResolvableModel.Resolver) {
        // block/block 是标准 Minecraft 模型，始终已加载，无需额外解析
    }

    override fun bake(context: ItemModel.BakeContext): ItemModel {
        val baker = context.blockModelBaker()
        val spriteGetter = baker.getSpriteGetter()
        val simpleModel = object : SimpleModel {
            override fun name() = "bedrock_item_geometry"
        }

        val sprites = mutableMapOf<String, Sprite>()
        geometryModel.materials.forEach { (key, material) ->
            sprites[key] = spriteGetter.get(material.spriteId, simpleModel)
        }
        val defaultSprite = sprites["*"] ?: sprites.values.firstOrNull()
            ?: throw IllegalStateException("No sprites available for bedrock item model")

        val modelPart = geometryModel.getModelPartForBaking()
        val mesh = BedrockRenderUtil.bakeModelPartToMesh(modelPart, defaultSprite, sprites, null)

        val quads = mutableListOf<BakedQuad>()
        mesh.forEach { quadView ->
            quads.add(quadView.toBakedQuad(defaultSprite))
        }

        // 从 block/block 模型获取标准方块显示变换（GUI 等轴测、手持角度等）
        val blockBlockModel = baker.getModel(identifierOf("block/block"))
        val transforms = blockBlockModel.getTransformations()

        val settings = ModelSettings(true, defaultSprite, transforms)
        //? if >=1.21.11 {
        // 直接实现 ItemModel 接口，避免反射访问 BasicItemModel 的包私有构造函数
        val renderLayer = TexturedRenderLayers.getEntityCutout()
        return object : ItemModel {
            override fun update(
                state: ItemRenderState,
                stack: ItemStack,
                resolver: ItemModelManager,
                displayContext: ItemDisplayContext,
                world: ClientWorld?,
                heldItemContext: HeldItemContext?,
                seed: Int
            ) {
                state.addModelKey(this)
                val layer = state.newLayer()
                if (stack.hasGlint()) {
                    layer.setGlint(ItemRenderState.Glint.STANDARD)
                }
                layer.setRenderLayer(renderLayer)
                settings.addSettings(layer, displayContext)
                layer.getQuads().addAll(quads)
            }
        }
        //?} else {
        /*return BasicItemModel(emptyList(), quads, settings)
        *///?}
    }
}
//?}
