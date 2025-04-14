package net.easecation.bedrockloader.mixin;

import net.minecraft.state.StateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.regex.Pattern;

@Mixin(StateManager.class)
public class StateManagerMixin {
    @Shadow
    static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_:]+$");
}
