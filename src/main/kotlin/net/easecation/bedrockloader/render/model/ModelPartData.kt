/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package net.easecation.bedrockloader.render.model

import com.google.common.collect.ImmutableList
import com.google.common.collect.Maps
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import java.util.stream.Collectors

@Environment(value = EnvType.CLIENT)
class ModelPartData internal constructor(private val cuboidData: List<ModelCuboidData?>?, private val rotationData: ModelTransform) {
    private val children: MutableMap<String, ModelPartData> = Maps.newHashMap()

    fun addChild(name: String, builder: ModelPartBuilder, rotationData: ModelTransform): ModelPartData {
        val modelPartData = ModelPartData(builder.build(), rotationData)
        val previousModelPartData = children.put(name, modelPartData)
        if (previousModelPartData != null) {
            modelPartData.children.putAll(previousModelPartData.children)
        }
        return modelPartData
    }

    fun createPart(textureWidth: Int, textureHeight: Int): ModelPart {
        val object2ObjectArrayMap = children.entries.stream()
                .collect(Collectors.toMap(
                        { entry -> entry.key },
                        { entry -> entry.value.createPart(textureWidth, textureHeight) },
                        { modelPart, _ -> modelPart },
                        { Object2ObjectArrayMap() }
                ))

        val list = cuboidData!!.stream()
                .map { modelCuboidData -> modelCuboidData!!.createCuboid(textureWidth, textureHeight) }
                .collect(ImmutableList.toImmutableList())

        val modelPart = ModelPart(list, object2ObjectArrayMap)
        modelPart.transform = rotationData
        return modelPart
    }

    fun getChild(name: String): ModelPartData? {
        return children[name]
    }
}

