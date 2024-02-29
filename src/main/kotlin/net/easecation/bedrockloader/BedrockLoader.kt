package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


object BedrockLoader : ModInitializer {

    val logger: Logger = LoggerFactory.getLogger("bedrock-loader")

	const val NAMESPACE: String = "bedrock-loader"
	val BEDROCK_LOADER_MOD: ModContainer = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow()

	override fun onInitialize() {
		logger.info("Initializing BedrockLoader...")

		logger.info("Loading bedrock addons...")
		BedrockAddonsLoader.load()

		// group
		var iconItem = Items.BONE_BLOCK
		if (BedrockAddonsRegistry.items.isNotEmpty()) {
			iconItem = BedrockAddonsRegistry.items.values.first()
		}
		FabricItemGroupBuilder.create(Identifier("bedrock-loader", "bedrock-loader"))
				.icon { ItemStack(iconItem) }
				.appendItems { stacks ->
					BedrockAddonsRegistry.items.forEach { (_, item) ->
						stacks.add(ItemStack(item))
					}
				}
				.build()

		logger.info("BedrockLoader initialized!")

		// 测试
		test()
	}

	private fun test() {

	}

	fun getGameDir(): File {
		return FabricLoader.getInstance().gameDir.toFile()
	}

	fun getTmpResourceDir(): File {
		return File(getGameDir(), "bedrock-loader-resource")
	}
}