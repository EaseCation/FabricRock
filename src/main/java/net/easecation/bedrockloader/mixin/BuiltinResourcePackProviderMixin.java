package net.easecation.bedrockloader.mixin;

import net.easecation.bedrockloader.BedrockLoader;
import net.easecation.bedrockloader.resourcepack.BedrockLoaderResourcePackProvider;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ClientBuiltinResourcePackProvider.class)
public class BuiltinResourcePackProviderMixin {
	@Inject(method = "register",at=@At("RETURN"))
	public void register(Consumer<ResourcePackProfile> profileAdder, ResourcePackProfile.Factory factory, CallbackInfo ci){
		BedrockLoader.INSTANCE.getLogger().info("BuiltinResourcePackProviderMixin init");
		new BedrockLoaderResourcePackProvider().register(profileAdder, factory);
	}
}