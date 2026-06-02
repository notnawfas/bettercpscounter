package com.notnawfas.betterpvputils.hud;

import com.notnawfas.betterpvputils.config.ModConfig;
import com.notnawfas.betterpvputils.cps.CpsVariable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

public class CpsHudOverlay implements HudRenderCallback {

    private static final int PADDING = 2;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        ModConfig config = ModConfig.getConfig();
        if (!config.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String resolved = CpsVariable.resolve(config.displayFormat);
        Text displayText = Text.literal(resolved);

        int textWidth = client.textRenderer.getWidth(resolved);
        int textHeight = client.textRenderer.fontHeight;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int x = (int) (config.posX * (screenWidth - textWidth - PADDING * 2));
        int y = (int) (config.posY * (screenHeight - textHeight - PADDING * 2));

        x = Math.max(0, Math.min(x, screenWidth - textWidth - PADDING * 2));
        y = Math.max(0, Math.min(y, screenHeight - textHeight - PADDING * 2));

        if (config.showBackground) {
            drawContext.fill(x, y, x + textWidth + PADDING * 2, y + textHeight + PADDING * 2, config.backgroundColor);
        }

        drawContext.drawText(client.textRenderer, displayText, x + PADDING, y + PADDING, config.textColor, true);
    }

    public static void renderPreview(DrawContext drawContext, ModConfig config, int forcedX, int forcedY) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(config.displayFormat);
        Text displayText = Text.literal(resolved);

        int textWidth = client.textRenderer.getWidth(resolved);
        int textHeight = client.textRenderer.fontHeight;

        if (config.showBackground) {
            drawContext.fill(forcedX, forcedY, forcedX + textWidth + PADDING * 2, forcedY + textHeight + PADDING * 2, config.backgroundColor);
        }

        drawContext.drawText(client.textRenderer, displayText, forcedX + PADDING, forcedY + PADDING, config.textColor, true);
    }

    public static int getHudWidth(ModConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(config.displayFormat);
        return client.textRenderer.getWidth(resolved) + PADDING * 2;
    }

    public static int getHudHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight + PADDING * 2;
    }
}
