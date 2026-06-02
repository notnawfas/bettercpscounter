package com.notnawfas.betterpvputils.mixin;

import com.notnawfas.betterpvputils.cps.CpsTracker;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseHandlerMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        if (action == 1) {
            CpsTracker.INSTANCE.onMouseClick(mouseInput.button());
        }
    }
}
