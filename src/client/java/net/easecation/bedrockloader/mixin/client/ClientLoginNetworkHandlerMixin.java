package net.easecation.bedrockloader.mixin.client;

//? if >=1.21 {
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

// Sends fabricrock:confirm channel registration immediately after login success,
// so ViaBedrock detects FabricRock before sending any chunk data.
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
//?} else {
/*import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

// 1.20.4: FabricRock confirm mechanism not needed (1.21+ only feature for ViaBedrock)
// No-op mixin targeting MinecraftClient to satisfy the mixin framework.
@Mixin(MinecraftClient.class)
public class ClientLoginNetworkHandlerMixin {}
*///?}
