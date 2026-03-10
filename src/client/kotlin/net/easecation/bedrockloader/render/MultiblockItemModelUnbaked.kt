package net.easecation.bedrockloader.render

//? if >=1.21.5 {
import com.mojang.serialization.MapCodec
//? if <1.21.11 {
/*import net.minecraft.client.render.item.model.BasicItemModel
*///?}
import net.easecation.bedrockloader.loader.MultiblockItemData
import net.minecraft.client.render.item.model.ItemModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.ModelSettings
import net.minecraft.client.render.model.ResolvableModel
import net.minecraft.client.render.model.SimpleModel
import net.minecraft.client.texture.Sprite
import net.easecation.bedrockloader.util.identifierOf
//? if >=1.21.11 {
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.item.ItemRenderState
import net.minecraft.client.item.ItemModelManager
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.HeldItemContext
//?}

class MultiblockItemModelUnbaked(
    private val data: MultiblockItemData
) : ItemModel.Unbaked {

    override fun getCodec(): MapCodec<out ItemModel.Unbaked> {
        throw UnsupportedOperationException("MultiblockItemModelUnbaked is not serializable")
    }

    override fun resolve(resolver: ResolvableModel.Resolver) {}

    override fun bake(context: ItemModel.BakeContext): ItemModel {
        val baker = context.blockModelBaker()
        val spriteGetter = baker.getSpriteGetter()
        val simpleModel = object : SimpleModel {
            override fun name() = "bedrock_multiblock_item"
        }

        val meshesWithOffsets = data.parts.mapNotNull { (model, offset) ->
            val sprites = mutableMapOf<String, Sprite>()
            model.materials.forEach { (key, material) ->
                sprites[key] = spriteGetter.get(material.spriteId, simpleModel)
            }
            val defaultSprite = sprites["*"] ?: sprites.values.firstOrNull() ?: return@mapNotNull null
            val mesh = BedrockRenderUtil.bakeModelPartToMesh(model.getModelPartForBaking(), defaultSprite, sprites, model.getBlockTransformation())
            Pair(mesh, offset)
        }

        val combinedMesh = if (meshesWithOffsets.isNotEmpty()) {
            BedrockRenderUtil.combineMultiblockMeshes(meshesWithOffsets)
        } else {
            null
        }

        val firstSprite = data.parts.firstOrNull()?.first?.materials?.values?.firstOrNull()
            ?.let { spriteGetter.get(it.spriteId, simpleModel) }
            ?: throw IllegalStateException("No sprites available for multiblock item model")

        val quads = mutableListOf<BakedQuad>()
        combinedMesh?.forEach { quadView ->
            quads.add(quadView.toBakedQuad(firstSprite))
        }

        val blockBlockModel = baker.getModel(identifierOf("block/block"))
        val transforms = blockBlockModel.getTransformations()
        val settings = ModelSettings(true, firstSprite, transforms)

        //? if >=1.21.11 {
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
