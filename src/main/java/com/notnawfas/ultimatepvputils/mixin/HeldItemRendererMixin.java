package com.notnawfas.ultimatepvputils.mixin;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.config.ShieldConfig;
import com.notnawfas.ultimatepvputils.hud.ShieldCustomizeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
        )
    )
    private void onBeforeItemRender(
        LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode,
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
        CallbackInfo ci
    ) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ShieldItem)) return;
        if (!renderMode.isFirstPerson()) return;

        ShieldConfig cfg = ModConfig.getConfig().shieldConfig;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean onCustomize = client.currentScreen instanceof ShieldCustomizeScreen;

        if (!cfg.visible && !onCustomize) {
            matrices.scale(0, 0, 0);
            return;
        }

        matrices.translate((float) cfg.offsetX / 100F, (float) -cfg.offsetY / 100F, 0);
        float scale = (float) cfg.size;
        matrices.scale(scale, scale, scale);
    }
}
