package com.notnawfas.ultimatepvputils.mixin;

import com.notnawfas.ultimatepvputils.cps.CpsTracker;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardHandlerMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, KeyInput keyInput, CallbackInfo ci) {
        CpsTracker.INSTANCE.onKeyPress(keyInput.key(), key);
    }
}
