package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsLoader.context
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockBehaviorPackLoader
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


object BedrockLoader : ModInitializer {

    val logger: Logger = LoggerFactory.getLogger("bedrock-loader")

	val ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.key, Identifier("bedrock-loader", "bedrock-loader"))
	val ITEM_GROUP = FabricItemGroup.builder()
		.icon { ItemStack(BedrockAddonsRegistry.items.values.firstOrNull() ?: Items.BONE_BLOCK) }
		.displayName(Text.translatable("itemGroup.bedrock-loader.bedrock-loader"))
		.build()

	override fun onInitialize() {
		logger.info("Initializing BedrockLoader...")

		logger.info("Loading bedrock addons...")

		BedrockAddonsLoader.load()

		Registry.register(Registries.ITEM_GROUP, ITEM_GROUP_KEY, ITEM_GROUP);

		ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP_KEY)
			.register(ModifyEntries { itemGroup ->
				BedrockAddonsRegistry.items.values.forEach {
					itemGroup.add(ItemStack(it))
				}
			})

		// load behaviour pack
		logger.info("Loading behaviour pack...")
		val behaviorPackLoader = BedrockBehaviorPackLoader(context)
		behaviorPackLoader.load()

		logger.info("BedrockLoader initialized!")
	}

	fun getGameDir(): File {
		return FabricLoader.getInstance().gameDir.toFile()
	}

	fun getTmpResourceDir(): File {
		return File(getGameDir(), "bedrock-loader-resource")
	}
}