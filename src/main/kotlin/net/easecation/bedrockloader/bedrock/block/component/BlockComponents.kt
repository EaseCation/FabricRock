package net.easecation.bedrockloader.bedrock.block.component

import com.google.gson.annotations.SerializedName

data class BlockComponents(
    @SerializedName("minecraft:material_instances") val minecraftMaterialInstances: ComponentMaterialInstances?,
    @SerializedName("minecraft:collision_box") val minecraftCollisionBox: ComponentCollisionBox?,
    @SerializedName("minecraft:selection_box") val minecraftSelectionBox: ComponentSelectionBox?,
    @SerializedName("minecraft:display_name") val minecraftDisplayName: String?,
    @SerializedName("minecraft:geometry") val minecraftGeometry: ComponentGeometry?,
    @SerializedName("minecraft:placement_filter") val minecraftPlacementFilter: ComponentPlacementFilter?,
    @SerializedName("minecraft:light_emission") val minecraftLightEmission: Int?,
    @SerializedName("minecraft:light_dampening") val minecraftLightDampening: Int?,
    @SerializedName("minecraft:destructible_by_mining") val minecraftDestructibleByMining: ComponentDestructibleByMining?,
    @SerializedName("minecraft:transformation") val minecraftTransformation: ComponentTransformation?,
    @SerializedName("netease:block_entity") val neteaseBlockEntity: ComponentBlockEntity?,
    @SerializedName("netease:face_directional") val neteaseFaceDirectional: ComponentFaceDirectional?,
) {
    fun mergeComponents(components: BlockComponents): BlockComponents {
        return BlockComponents(
            minecraftMaterialInstances = components.minecraftMaterialInstances ?: minecraftMaterialInstances,
            minecraftCollisionBox = components.minecraftCollisionBox ?: minecraftCollisionBox,
            minecraftSelectionBox = components.minecraftSelectionBox ?: minecraftSelectionBox,
            minecraftDisplayName = components.minecraftDisplayName ?: minecraftDisplayName,
            minecraftGeometry = components.minecraftGeometry ?: minecraftGeometry,
            minecraftPlacementFilter = components.minecraftPlacementFilter ?: minecraftPlacementFilter,
            minecraftLightEmission = components.minecraftLightEmission ?: minecraftLightEmission,
            minecraftLightDampening = components.minecraftLightDampening ?: minecraftLightDampening,
            minecraftDestructibleByMining = components.minecraftDestructibleByMining ?: minecraftDestructibleByMining,
            minecraftTransformation = components.minecraftTransformation ?: minecraftTransformation,
            neteaseBlockEntity = components.neteaseBlockEntity ?: neteaseBlockEntity,
            neteaseFaceDirectional = components.neteaseFaceDirectional ?: neteaseFaceDirectional,
        )
    }
}
