package net.easecation.bedrockloader.mixin;

import com.google.common.collect.ImmutableSet;
import net.easecation.bedrockloader.BedrockLoader;
import net.easecation.bedrockloader.resourcepack.BedrockLoaderResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
	@Mutable
	@Final
	@Shadow
	private Set<ResourcePackProvider> providers;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void register(CallbackInfo ci) {
		BedrockLoader.INSTANCE.getLogger().info("ResourcePackManagerMixin init");
		this.providers = new LinkedHashSet<>(this.providers);
		this.providers.add(new BedrockLoaderResourcePackProvider());
	}
}