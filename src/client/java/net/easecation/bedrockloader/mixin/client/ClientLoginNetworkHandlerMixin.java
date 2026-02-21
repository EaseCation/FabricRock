package net.easecation.bedrockloader.mixin.client;

import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Sends fabricrock:confirm channel registration immediately after login success,
 * so ViaBedrock detects FabricRock before sending any chunk data.
 * Without this, the Fabric API's automatic minecraft:register is sent at PLAY phase start,
 * which may arrive after chunks have already been processed (especially with shaders).
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientLoginNetworkHandler.class)
public class ClientLoginNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onSuccess", at = @At("RETURN"))
    private void bedrockLoader_sendEarlyConfirm(LoginSuccessS2CPacket packet, CallbackInfo ci) {
        this.connection.send(new CustomPayloadC2SPacket(
            new RegistrationPayload(
                RegistrationPayload.REGISTER,
                List.of(Identifier.of("fabricrock", "confirm"))
            )
        ));
    }
}
