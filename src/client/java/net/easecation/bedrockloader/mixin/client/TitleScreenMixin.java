package net.easecation.bedrockloader.mixin.client;

import net.easecation.bedrockloader.loader.BedrockPackRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject custom pack information display into the Minecraft title screen.
 * This mixin adds a text overlay showing the number of loaded Bedrock packs.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    @Final
    private boolean doBackgroundFade;

    @Shadow
    @Final
    private long backgroundFadeStart;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    /**
     * Injects custom rendering at the end of the TitleScreen render method.
     * Displays pack information in the bottom-right corner of the screen.
     *
     * @param context the drawing context
     * @param mouseX current mouse X position
     * @param mouseY current mouse Y position
     * @param delta frame time delta
     * @param ci callback info
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockLoader_renderPackInfo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Get pack counts from the registry
        int packCount = BedrockPackRegistry.INSTANCE.getPackCount();
        int resourcePackCount = BedrockPackRegistry.INSTANCE.getPacksByType("resources").size();
        int behaviorPackCount = BedrockPackRegistry.INSTANCE.getPacksByType("data").size();

        // Build display message
        String message = String.format("Bedrock Loader: %d packs (%d resources, %d behaviors)",
                                       packCount, resourcePackCount, behaviorPackCount);

        // Calculate text position (bottom-right corner, with 2px margin)
        int textWidth = this.textRenderer.getWidth(message);
        int textX = this.width - textWidth - 2;
        int textY = this.height - 20;

        // Calculate fade-in alpha to match the background fade effect
        float fadeProgress = this.doBackgroundFade ?
            (float)(Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;
        float alpha = this.doBackgroundFade ?
            MathHelper.clamp(fadeProgress - 1.0F, 0.0F, 1.0F) : 1.0F;
        int alphaValue = MathHelper.ceil(alpha * 255.0F) << 24;

        // Render text with shadow (white color with calculated alpha)
        context.drawTextWithShadow(this.textRenderer, message, textX, textY, 0xFFFFFF | alphaValue);
    }
}
