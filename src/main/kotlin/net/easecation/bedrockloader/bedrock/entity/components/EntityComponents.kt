package net.easecation.bedrockloader.bedrock.entity.components

import com.google.gson.annotations.SerializedName

data class EntityComponents(
        @SerializedName("minecraft:physics") val minecraftPhysics: ComponentPhysics?,
        @SerializedName("minecraft:scale") val minecraftScale: ComponentScale?,
        @SerializedName("minecraft:type_family") val minecraftTypeFamily: ComponentTypeFamily?,
        @SerializedName("minecraft:movement") val minecraftMovement: ComponentMovement?,
        @SerializedName("minecraft:movement.basic") val minecraftMovementBasic: ComponentMovementBasic?,
        @SerializedName("minecraft:knockback_resistance") val minecraftKnockbackResistance: ComponentKnockbackResistance?,
        @SerializedName("minecraft:navigation.walk") val minecraftNavigationWalk: ComponentNavigationWalk?,
        @SerializedName("minecraft:is_baby") val minecraftIsBaby: Boolean?,
        @SerializedName("minecraft:is_ignited") val minecraftIsIgnited: Boolean?,
        @SerializedName("minecraft:is_saddled") val minecraftIsSaddled: Boolean?,
        @SerializedName("minecraft:is_sheared") val minecraftIsSheared: Boolean?,
        @SerializedName("minecraft:is_tamed") val minecraftIsTamed: Boolean?,
        @SerializedName("minecraft:is_illager_captain") val minecraftIsIllagerCaptain: Boolean?,
        @SerializedName("minecraft:variant") val minecraftVariant: Int?,
        @SerializedName("minecraft:mark_variant") val minecraftMarkVariant: Int?,
        @SerializedName("minecraft:skin_id") val minecraftSkinId: Int?,
        @SerializedName("minecraft:health") val minecraftHealth: ComponentHealth?,
        @SerializedName("minecraft:rideable") val minecraftRideable: ComponentRideable?,
        @SerializedName("minecraft:is_immobile") val minecraftIsImmobile: ComponentIsImmobile?,
        @SerializedName("minecraft:pushable") val minecraftPushable: ComponentPushable?,
)