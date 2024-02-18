package net.easecation.bedrockloader.entity

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Arm
import net.minecraft.util.Identifier
import net.minecraft.world.World

class EntityDataDriven(
        val identifier: Identifier,
        val components: EntityComponents,
        entityType: EntityType<out MobEntity>?,
        world: World?
) : MobEntity(entityType, world) {

    companion object {
        fun buildEntityType(identifier: Identifier): EntityType<EntityDataDriven> {
            val builder = FabricEntityTypeBuilder.create(SpawnGroup.CREATURE) { type, world ->
                val components = BedrockAddonsRegistry.entityComponents[identifier]
                        ?: throw IllegalStateException("[EntityDataDriven] Entity $identifier has no components")
                EntityDataDriven(identifier, components, type, world)
            }
            return builder.build()
        }
        fun buildEntityAttributes(components: EntityComponents): DefaultAttributeContainer.Builder {
            val builder = MobEntity.createMobAttributes()
            // TODO components
            return builder
        }
    }

    override fun getArmorItems(): MutableIterable<ItemStack> {
        return mutableListOf()
    }

    override fun equipStack(slot: EquipmentSlot?, stack: ItemStack?) {
    }

    override fun getEquippedStack(slot: EquipmentSlot?): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getMainArm(): Arm {
        return Arm.RIGHT
    }


}