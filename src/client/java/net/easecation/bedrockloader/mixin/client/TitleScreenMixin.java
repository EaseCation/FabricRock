package net.easecation.bedrockloader.mixin.client;

import net.easecation.bedrockloader.loader.BedrockPackRegistry;
import net.easecation.bedrockloader.loader.error.LoadingErrorCollector;
import net.easecation.bedrockloader.screen.LoadingErrorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /**
     * 错误提示文本的边界，用于点击检测
     * [x1, y1, x2, y2]
     */
    @Unique
    private int[] bedrockLoader_errorTextBounds = null;

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

        // 渲染错误提示（如果有，在包信息上方）
        if (LoadingErrorCollector.INSTANCE.hasErrors()) {
            int errorCount = LoadingErrorCollector.INSTANCE.getErrorCount();
            int warningCount = LoadingErrorCollector.INSTANCE.getWarningCount();
            int totalCount = errorCount + warningCount;

            // 构建错误提示文本
            String errorMessage = "! " + totalCount + " loading issues [click to view]";

            // 计算位置（在包信息上方）
            int errorTextWidth = this.textRenderer.getWidth(errorMessage);
            int errorTextX = this.width - errorTextWidth - 2;
            int errorTextY = textY - 10; // 在包信息上方10像素

            // 颜色：有ERROR时红色，只有WARNING时黄色
            int color = errorCount > 0 ? 0xFF5555 : 0xFFAA00;

            // 检查鼠标是否悬停
            boolean isHovered = mouseX >= errorTextX && mouseX <= errorTextX + errorTextWidth &&
                               mouseY >= errorTextY && mouseY <= errorTextY + 10;
            if (isHovered) {
                // 悬停时使用更亮的颜色
                color = errorCount > 0 ? 0xFF7777 : 0xFFCC00;
            }

            // 渲染错误提示
            context.drawTextWithShadow(this.textRenderer, errorMessage, errorTextX, errorTextY, color | alphaValue);

            // 保存边界用于点击检测
            bedrockLoader_errorTextBounds = new int[]{errorTextX, errorTextY, errorTextX + errorTextWidth, errorTextY + 10};
        } else {
            bedrockLoader_errorTextBounds = null;
        }

        // Render text with shadow (white color with calculated alpha)
        context.drawTextWithShadow(this.textRenderer, message, textX, textY, 0xFFFFFF | alphaValue);
    }

    /**
     * 处理鼠标点击事件
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bedrockLoader_handleErrorClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && bedrockLoader_errorTextBounds != null) {
            if (mouseX >= bedrockLoader_errorTextBounds[0] && mouseX <= bedrockLoader_errorTextBounds[2] &&
                mouseY >= bedrockLoader_errorTextBounds[1] && mouseY <= bedrockLoader_errorTextBounds[3]) {
                // 打开错误详情界面
                MinecraftClient.getInstance().setScreen(new LoadingErrorScreen((TitleScreen)(Object)this));
                cir.setReturnValue(true);
            }
        }
    }
}
