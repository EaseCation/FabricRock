package net.easecation.bedrockloader.entity

import net.easecation.bedrockloader.bedrock.entity.components.EntityComponents
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.minecraft.entity.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Arm
import net.minecraft.util.Identifier
import net.minecraft.world.World

class EntityDataDriven(
        val identifier: Identifier,
        val components: EntityComponents,
        entityType: EntityType<EntityDataDriven>,
        world: World
) : MobEntity(entityType, world) {

    /**
     * 动画管理器 - 仅在客户端存在
     * 类型为 EntityAnimationManager，但由于是客户端类，这里声明为 Any?
     */
    var animationManager: Any? = null

    companion object {
        fun buildEntityType(identifier: Identifier): EntityType<EntityDataDriven> {
            //? if >=1.21.2 {
            val registryKey = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.ENTITY_TYPE, identifier)
            return EntityType.Builder.create({ type, world ->
                val components = BedrockAddonsRegistry.entityComponents[identifier]
                    ?: throw IllegalStateException("[EntityDataDriven] Entity $identifier has no components")
                EntityDataDriven(identifier, components, type, world)
            }, SpawnGroup.CREATURE)
                .dimensions(1f, 1f)
                .build(registryKey)
            //?} elif >=1.21 {
            /*return EntityType.Builder.create({ type, world ->
                val components = BedrockAddonsRegistry.entityComponents[identifier]
                    ?: throw IllegalStateException("[EntityDataDriven] Entity $identifier has no components")
                EntityDataDriven(identifier, components, type, world)
            }, SpawnGroup.CREATURE)
                .dimensions(1f, 1f)
                .build(identifier.toString())
            *///?} else {
            /*return EntityType.Builder.create({ type, world ->
                val components = BedrockAddonsRegistry.entityComponents[identifier]
                    ?: throw IllegalStateException("[EntityDataDriven] Entity $identifier has no components")
                EntityDataDriven(identifier, components, type, world)
            }, SpawnGroup.CREATURE).apply {
                setDimensions(1f, 1f)
            }.build()
            *///?}
        }
        fun buildEntityAttributes(components: EntityComponents): DefaultAttributeContainer.Builder {
            val builder = createMobAttributes()
            // TODO components
            // Add HealthComponent, KnockbackResistanceComponent, MovementComponent
            components.let {
                //? if >=1.21.2 {
                it.minecraftHealth?.max?.let { value -> builder.add(EntityAttributes.MAX_HEALTH, value.toDouble()) } ?:
                it.minecraftHealth?.value?.let { value -> builder.add(EntityAttributes.MAX_HEALTH, value.toDouble()) }
                it.minecraftKnockbackResistance?.value?.let { value -> builder.add(EntityAttributes.KNOCKBACK_RESISTANCE, value.toDouble()) }
                it.minecraftMovement?.value?.let { value ->  builder.add(EntityAttributes.MOVEMENT_SPEED, value.toDouble()) }
                //?} else {
                /*it.minecraftHealth?.max?.let { value -> builder.add(EntityAttributes.GENERIC_MAX_HEALTH, value.toDouble()) } ?:
                it.minecraftHealth?.value?.let { value -> builder.add(EntityAttributes.GENERIC_MAX_HEALTH, value.toDouble()) }
                it.minecraftKnockbackResistance?.value?.let { value -> builder.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, value.toDouble()) }
                it.minecraftMovement?.value?.let { value ->  builder.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, value.toDouble()) }
                *///?}
            }
            return builder
        }
    }

    override fun isPushable(): Boolean {
        return components.minecraftPushable?.is_pushable ?: false
    }

    override fun pushAwayFrom(entity: Entity?) {
        if (this.isPushable) {
            super.pushAwayFrom(entity)
        }
    }

    override fun pushAway(entity: Entity?) {
        if (this.isPushable) {
            super.pushAway(entity)
        }
    }

    //? if <1.21.5 {
    /*override fun getArmorItems(): MutableIterable<ItemStack> {
        return mutableListOf()
    }
    *///?}

    override fun equipStack(slot: EquipmentSlot?, stack: ItemStack?) {
    }

    override fun getEquippedStack(slot: EquipmentSlot?): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getMainArm(): Arm {
        return Arm.RIGHT
    }

    override fun hasNoGravity(): Boolean {
        return components.minecraftPhysics?.has_gravity == false
    }

    //? if >=1.21.2 {
    override fun damage(world: net.minecraft.server.world.ServerWorld, source: DamageSource, amount: Float): Boolean {
        return components.minecraftHealth?.min?.let {
            when {
                health - amount > 1 && !source.isOf(DamageTypes.OUT_OF_WORLD) -> {
                    return super.damage(world, source, amount)
                }
                else -> {
                    if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
                        return false
                    }
                    health += amount
                    return super.damage(world, source, amount)
                }
            }
        }?: return super.damage(world, source, amount)
    }
    //?} else {
    /*override fun damage(source: DamageSource, amount: Float): Boolean {
        return components.minecraftHealth?.min?.let {
            when {
                health - amount > 1 && !source.isOf(DamageTypes.OUT_OF_WORLD) -> {
                    return super.damage(source, amount)
                }
                else -> {
                    if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
                        return false
                    }
                    health += amount
                    return super.damage(source, amount)
                }
            }
        }?: return super.damage(source, amount)
    }
    *///?}

}