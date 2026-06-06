package com.notnawfas.ultimatepvputils;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.config.ModConfigScreen;
import com.notnawfas.ultimatepvputils.hud.CpsHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class UltimatePvPUtilsClient implements ClientModInitializer {

    private static KeyBinding configKey;

    private static final KeyBinding.Category MOD_CATEGORY = KeyBinding.Category.create(Identifier.of("ultimatepvputils", "keybindings"));

    private static boolean hasShownHint = false;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new CpsHudOverlay());

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ultimatepvputils.open_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            MOD_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKey.wasPressed()) {
                client.setScreen(new ModConfigScreen(null));
            }
            if (!hasShownHint && client.player != null) {
                hasShownHint = true;
                client.player.sendMessage(Text.literal("§b[Ultimate PvP Utils]§7 Press §bF7§7 to open config"), false);
            }
        });
    }
}
