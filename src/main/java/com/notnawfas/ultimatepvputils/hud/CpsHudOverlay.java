package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.cps.CpsVariable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

public class CpsHudOverlay implements HudRenderCallback {

    private static final int INNER_PADDING = 4;
    private static final int BORDER_WIDTH = 1;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        ModConfig config = ModConfig.getConfig();
        if (!config.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String resolved = CpsVariable.resolve(config.displayFormat);
        Text displayText = Text.literal(resolved);

        float scale = (float) Math.max(0.5, Math.min(2.0, config.scale));
        int textWidth = client.textRenderer.getWidth(resolved);
        int textHeight = client.textRenderer.fontHeight;

        int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
        int totalHeight = textHeight + (INNER_PADDING + BORDER_WIDTH) * 2;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int scaledWidth = (int) (totalWidth * scale);
        int scaledHeight = (int) (totalHeight * scale);

        int x = (int) (config.posX * (screenWidth - scaledWidth));
        int y = (int) (config.posY * (screenHeight - scaledHeight));

        x = Math.max(0, Math.min(x, screenWidth - scaledWidth));
        y = Math.max(0, Math.min(y, screenHeight - scaledHeight));

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(x, y);
        drawContext.getMatrices().scale(scale, scale);

        if (config.showBackground) {
            int borderColor = deriveBorderColor(config.backgroundColor);
            drawContext.fill(0, 0, totalWidth, totalHeight, borderColor);
            drawContext.fill(BORDER_WIDTH, BORDER_WIDTH,
                totalWidth - BORDER_WIDTH, totalHeight - BORDER_WIDTH,
                config.backgroundColor);
        }

        drawContext.drawText(client.textRenderer, displayText,
            INNER_PADDING + BORDER_WIDTH,
            INNER_PADDING + BORDER_WIDTH,
            config.textColor, true);

        drawContext.getMatrices().popMatrix();
    }

    public static void renderPreview(DrawContext drawContext, ModConfig config, int forcedX, int forcedY) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(config.displayFormat);
        Text displayText = Text.literal(resolved);

        float scale = (float) Math.max(0.5, Math.min(2.0, config.scale));
        int textWidth = client.textRenderer.getWidth(resolved);
        int textHeight = client.textRenderer.fontHeight;

        int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
        int totalHeight = textHeight + (INNER_PADDING + BORDER_WIDTH) * 2;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(forcedX, forcedY);
        drawContext.getMatrices().scale(scale, scale);

        if (config.showBackground) {
            int borderColor = deriveBorderColor(config.backgroundColor);
            drawContext.fill(0, 0, totalWidth, totalHeight, borderColor);
            drawContext.fill(BORDER_WIDTH, BORDER_WIDTH,
                totalWidth - BORDER_WIDTH, totalHeight - BORDER_WIDTH,
                config.backgroundColor);
        }

        drawContext.drawText(client.textRenderer, displayText,
            INNER_PADDING + BORDER_WIDTH,
            INNER_PADDING + BORDER_WIDTH,
            config.textColor, true);

        drawContext.getMatrices().popMatrix();
    }

    public static int getHudWidth(ModConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(config.displayFormat);
        float scale = (float) Math.max(0.5, Math.min(2.0, config.scale));
        int textWidth = client.textRenderer.getWidth(resolved);
        int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
        return (int) (totalWidth * scale);
    }

    public static int getHudHeight() {
        float scale = (float) Math.max(0.5, Math.min(2.0, ModConfig.getConfig().scale));
        int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;
        int totalHeight = textHeight + (INNER_PADDING + BORDER_WIDTH) * 2;
        return (int) (totalHeight * scale);
    }

    private static int deriveBorderColor(int bgColor) {
        int a = Math.min(255, ((bgColor >> 24) & 0xFF) + 40);
        int r = Math.min(255, ((bgColor >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((bgColor >> 8) & 0xFF) + 30);
        int b = Math.min(255, (bgColor & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
