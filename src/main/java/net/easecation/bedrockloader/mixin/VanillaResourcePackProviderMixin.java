package net.easecation.bedrockloader.mixin;

import net.easecation.bedrockloader.BedrockLoader;
import net.easecation.bedrockloader.resourcepack.BedrockLoaderResourcePackProvider;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.VanillaResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(VanillaResourcePackProvider.class)
public class VanillaResourcePackProviderMixin {
	@Inject(method = "register",at=@At("RETURN"))
	public void register(Consumer<ResourcePackProfile> profileAdder, CallbackInfo ci){
		BedrockLoader.INSTANCE.getLogger().info("VanillaResourcePackProviderMixin init");
		new BedrockLoaderResourcePackProvider().register(profileAdder);
	}
}