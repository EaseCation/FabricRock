package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader
import net.easecation.bedrockloader.loader.BedrockAddonsLoader.context
import net.easecation.bedrockloader.loader.BedrockAddonsRegistry
import net.easecation.bedrockloader.loader.BedrockBehaviorPackLoader
import net.easecation.bedrockloader.loader.BlockStateMappingExporter
import net.easecation.bedrockloader.sync.server.ConfigLoader
import net.easecation.bedrockloader.sync.server.EmbeddedHttpServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.Item
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

	// Remote Pack Sync HTTP Server
	private var httpServer: EmbeddedHttpServer? = null

	override fun onInitialize() {
		logger.info("Initializing BedrockLoader...")

		logger.info("Loading bedrock addons...")
		BedrockAddonsLoader.load()

		// load behaviour pack
		logger.info("Loading behaviour pack...")
		val behaviorPackLoader = BedrockBehaviorPackLoader(context)
		behaviorPackLoader.load()

		// 导出方块状态映射（用于 ViaBedrock 同步）
		logger.info("Exporting block state mappings...")
		BlockStateMappingExporter.export()

		// 动态创建每个包的创造模式选项卡
		logger.info("Creating item groups for loaded packs...")
		createPackItemGroups()

		// 启动HTTP服务器（仅在专用服务器上）
		startHttpServerIfNeeded()

		logger.info("BedrockLoader initialized!")
	}

	/**
	 * 为每个加载的包创建独立的创造模式选项卡
	 */
	private fun createPackItemGroups() {
		// 按包的加载顺序创建选项卡
		context.packs.forEach { packContext ->
			val packId = packContext.packId
			val packInfo = packContext.packInfo

			// 仅为data包创建选项卡
			if (packInfo.type != "data") return@forEach

			// 获取该包的所有物品
			val packItems = BedrockAddonsRegistry.getItemsByPack(packId)

			// 仅为有物品的包创建选项卡
			if (packItems.isEmpty()) {
				logger.debug("跳过空包: ${packInfo.name} [$packId]")
				return@forEach
			}

			// 智能选择图标
			val iconItem = selectIconItem(packId) ?: packItems.first()

			// 创建唯一的RegistryKey
			val groupKey = RegistryKey.of(
				Registries.ITEM_GROUP.key,
				Identifier.of("bedrock-loader", "pack_${packId.substring(0, 8)}")
			)

			// 创建ItemGroup
			val itemGroup = FabricItemGroup.builder()
				.icon { ItemStack(iconItem) }
				.displayName(Text.literal(packInfo.name))
				.build()

			// 注册ItemGroup
			Registry.register(Registries.ITEM_GROUP, groupKey, itemGroup)
			logger.info("创建创造模式选项卡: ${packInfo.name} [$packId] (${packItems.size} 个物品)")

			// 添加物品到选项卡
			ItemGroupEvents.modifyEntriesEvent(groupKey)
				.register { itemGroup ->
					packItems.forEach { item ->
						itemGroup.add(ItemStack(item))
					}
				}
		}
	}

	/**
	 * 智能选择图标物品
	 * 优先级: 方块 > 工具/武器 > 其他物品 > 刷怪蛋
	 */
	private fun selectIconItem(packId: String): Item? {
		val allItems = BedrockAddonsRegistry.getItemsByPack(packId)
		if (allItems.isEmpty()) return null

		// 优先级1: 方块物品
		val blockItems = BedrockAddonsRegistry.getBlockItemsByPack(packId)
		if (blockItems.isNotEmpty()) return blockItems.first()

		// 优先级2: 工具/武器（检查类名）
		val toolOrWeapon = allItems.find { item ->
			val className = item.javaClass.simpleName
			className.contains("Tool") || className.contains("Sword") ||
			className.contains("Axe") || className.contains("Pickaxe")
		}
		if (toolOrWeapon != null) return toolOrWeapon

		// 优先级3: 其他物品（非刷怪蛋）
		val nonSpawnEgg = allItems.find { it !is net.minecraft.item.SpawnEggItem }
		if (nonSpawnEgg != null) return nonSpawnEgg

		// 最后：刷怪蛋
		return allItems.first()
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