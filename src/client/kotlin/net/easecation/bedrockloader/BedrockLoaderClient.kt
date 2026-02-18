package net.easecation.bedrockloader

import net.easecation.bedrockloader.loader.BedrockAddonsLoader.context
import net.easecation.bedrockloader.loader.BedrockResourcePackLoader
import net.easecation.bedrockloader.render.BedrockModelLoadingPlugin
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Environment(EnvType.CLIENT)
object BedrockLoaderClient : ClientModInitializer {

    val logger: Logger = LoggerFactory.getLogger("bedrock-loader")

    /** Empty payload used to announce FabricRock presence to ViaBedrock proxy. */
    class FabricRockConfirmPayload : CustomPayload {
        companion object {
            val ID: CustomPayload.Id<FabricRockConfirmPayload> =
                CustomPayload.Id<FabricRockConfirmPayload>(Identifier.of("fabricrock", "confirm"))
            val CODEC: PacketCodec<RegistryByteBuf, FabricRockConfirmPayload> = object : PacketCodec<RegistryByteBuf, FabricRockConfirmPayload> {
                override fun decode(buf: RegistryByteBuf): FabricRockConfirmPayload = FabricRockConfirmPayload()
                override fun encode(buf: RegistryByteBuf, value: FabricRockConfirmPayload) {}
            }
        }
        override fun getId(): CustomPayload.Id<out CustomPayload> = ID
    }

    override fun onInitializeClient() {
        ModelLoadingPlugin.register(BedrockModelLoadingPlugin)

        // Register FabricRock confirm channel so ViaBedrock can detect this mod
        PayloadTypeRegistry.playS2C().register(FabricRockConfirmPayload.ID, FabricRockConfirmPayload.CODEC)
        ClientPlayNetworking.registerGlobalReceiver(FabricRockConfirmPayload.ID) { _, _ -> }

        // load resource pack
        logger.info("Loading resource pack...")
        val resourcePackLoader = BedrockResourcePackLoader(BedrockLoader.getTmpResourceDir(), context)
        resourcePackLoader.load()
    }

}