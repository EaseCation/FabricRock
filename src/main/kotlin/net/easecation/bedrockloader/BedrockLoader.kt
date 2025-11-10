package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsLoader.context
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockBehaviorPackLoader
import net.easecation.bedrockloader.sync.server.ConfigLoader
import net.easecation.bedrockloader.sync.server.EmbeddedHttpServer
import net.fabricmc.api.EnvType
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

	// Remote Pack Sync HTTP Server
	private var httpServer: EmbeddedHttpServer? = null

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

		// 启动HTTP服务器（仅在专用服务器上）
		startHttpServerIfNeeded()

		logger.info("BedrockLoader initialized!")
	}

	/**
	 * 启动HTTP服务器（如果需要）
	 *
	 * 仅在专用服务器(Dedicated Server)环境下启动HTTP服务器
	 * 客户端和集成服务器不启动
	 */
	private fun startHttpServerIfNeeded() {
		// 检查是否为服务端环境
		val envType = FabricLoader.getInstance().environmentType
		if (envType != EnvType.SERVER) {
			logger.debug("当前环境为客户端，不启动HTTP服务器")
			return
		}

		try {
			// 加载配置文件
			val configDir = File(getGameDir(), "config/bedrock-loader")
			val configFile = File(configDir, "server.yml")
			val config = ConfigLoader.loadServerConfig(configFile)

			// 检查是否启用
			if (!config.enabled) {
				logger.info("HTTP服务器已禁用（配置文件: enabled=false）")
				return
			}

			// 创建并启动HTTP服务器
			val packDirectory = File(getGameDir(), "config/bedrock-loader")
			httpServer = EmbeddedHttpServer(config, packDirectory)
			httpServer?.start()

			// 注册shutdown hook以实现优雅关闭
			Runtime.getRuntime().addShutdownHook(Thread {
				logger.info("正在关闭HTTP服务器...")
				httpServer?.stop()
			})

		} catch (e: Exception) {
			logger.error("启动HTTP服务器失败", e)
		}
	}

	fun getGameDir(): File {
		return FabricLoader.getInstance().gameDir.toFile()
	}

	fun getTmpResourceDir(): File {
		return File(getGameDir(), "bedrock-loader-resource")
	}
}