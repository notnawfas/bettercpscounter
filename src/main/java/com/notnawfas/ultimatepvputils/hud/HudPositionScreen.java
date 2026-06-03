package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudPositionScreen extends Screen {

    private final Screen parent;
    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private static final String[][] PRESET_LABELS = {
            {"Top-Left", "Top-Center", "Top-Right"},
            {"Mid-Left", "Center", "Mid-Right"},
            {"Bottom-Left", "Bottom-Center", "Bottom-Right"}
    };
    private static final double[][] PRESET_X = {
            {0.0, 0.5, 1.0},
            {0.0, 0.5, 1.0},
            {0.0, 0.5, 1.0}
    };
    private static final double[][] PRESET_Y = {
            {0.0, 0.0, 0.0},
            {0.5, 0.5, 0.5},
            {1.0, 1.0, 1.0}
    };

    private int selectedPresetRow = -1;
    private int selectedPresetCol = -1;

    public HudPositionScreen(Screen parent) {
        super(Text.translatable("ultimatepvputils.screen.reposition.title"));
        this.parent = parent;
        detectSelectedPreset();
    }

    private void detectSelectedPreset() {
        ModConfig config = ModConfig.getConfig();
        selectedPresetRow = -1;
        selectedPresetCol = -1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (Math.abs(config.posX - PRESET_X[r][c]) < 0.01 && Math.abs(config.posY - PRESET_Y[r][c]) < 0.01) {
                    selectedPresetRow = r;
                    selectedPresetCol = c;
                    return;
                }
            }
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            this.client.gameRenderer.renderWorld(this.client.getRenderTickCounter());
        }

        ModConfig config = ModConfig.getConfig();

        int hudWidth = CpsHudOverlay.getHudWidth(config);
        int hudHeight = CpsHudOverlay.getHudHeight();

        int hudX = (int) (config.posX * (width - hudWidth));
        int hudY = (int) (config.posY * (height - hudHeight));
        hudX = Math.max(0, Math.min(hudX, width - hudWidth));
        hudY = Math.max(0, Math.min(hudY, height - hudHeight));

        CpsHudOverlay.renderPreview(drawContext, config, hudX, hudY);

        long time = System.currentTimeMillis();
        float dashPhase = (time % 2000) / 2000.0f;
        drawAnimatedBorder(drawContext, hudX - 2, hudY - 2, hudWidth + 4, hudHeight + 4, 0xFFFFFFFF, dashPhase);

        int barH = 90;
        int barY = height - barH;
        drawContext.fill(0, barY, width, height, 0xB0000000);
        drawContext.fill(0, barY, width, barY + 1, 0x40FFFFFF);

        drawContext.drawCenteredTextWithShadow(textRenderer, Text.translatable("ultimatepvputils.screen.reposition.title"), width / 2, barY + 4, 0xFFFFFF);
        drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, barY + 16, 0xFFAAAAAA);

        int cellW = 80;
        int cellH = 28;
        int gridW = cellW * 3 + 8;
        int gridStartX = (width - gridW) / 2;
        int gridStartY = barY + 32;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = gridStartX + col * (cellW + 4);
                int cy = gridStartY + row * (cellH + 2);
                boolean hovered = mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH;
                boolean selected = (row == selectedPresetRow && col == selectedPresetCol);

                int bg;
                if (selected) {
                    bg = 0x804FC3F7;
                } else if (hovered) {
                    bg = 0x40FFFFFF;
                } else {
                    bg = 0x30000000;
                }
                drawContext.fill(cx, cy, cx + cellW, cy + cellH, bg);

                int borderCol = selected ? 0xFF4FC3F7 : 0x60FFFFFF;
                drawContext.fill(cx, cy, cx + cellW, cy + 1, borderCol);
                drawContext.fill(cx, cy + cellH - 1, cx + cellW, cy + cellH, borderCol);
                drawContext.fill(cx, cy, cx + 1, cy + cellH, borderCol);
                drawContext.fill(cx + cellW - 1, cy, cx + cellW, cy + cellH, borderCol);

                int textCol = selected ? 0xFF4FC3F7 : 0xFFCCCCCC;
                drawContext.drawCenteredTextWithShadow(textRenderer, PRESET_LABELS[row][col], cx + cellW / 2, cy + cellH / 2 - 4, textCol);
            }
        }
    }

    private void drawAnimatedBorder(DrawContext drawContext, int x, int y, int w, int h, int color, float phase) {
        int dashLen = 6;
        int gapLen = 4;
        int period = dashLen + gapLen;
        int offset = (int) (phase * period * 4);

        drawDashedLineH(drawContext, x, y, w, color, dashLen, gapLen, offset);
        drawDashedLineH(drawContext, x, y + h - 1, w, color, dashLen, gapLen, offset);
        drawDashedLineV(drawContext, x, y, h, color, dashLen, gapLen, offset);
        drawDashedLineV(drawContext, x + w - 1, y, h, color, dashLen, gapLen, offset);
    }

    private void drawDashedLineH(DrawContext drawContext, int x, int y, int w, int color, int dashLen, int gapLen, int offset) {
        int period = dashLen + gapLen;
        int pos = -offset % period;
        if (pos < 0) pos += period;
        while (pos < w) {
            int startX = x + pos;
            int endX = Math.min(x + pos + dashLen, x + w);
            if (startX < x + w) {
                drawContext.fill(startX, y, endX, y + 1, color);
            }
            pos += period;
        }
    }

    private void drawDashedLineV(DrawContext drawContext, int x, int y, int h, int color, int dashLen, int gapLen, int offset) {
        int period = dashLen + gapLen;
        int pos = -offset % period;
        if (pos < 0) pos += period;
        while (pos < h) {
            int startY = y + pos;
            int endY = Math.min(y + pos + dashLen, y + h);
            if (startY < y + h) {
                drawContext.fill(x, startY, x + 1, endY, color);
            }
            pos += period;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, always);
        }

        double mouseX = click.x();
        double mouseY = click.y();

        int barH = 90;
        int barY = height - barH;

        int cellW = 80;
        int cellH = 28;
        int gridW = cellW * 3 + 8;
        int gridStartX = (width - gridW) / 2;
        int gridStartY = barY + 32;

        if (mouseY >= gridStartY) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int cx = gridStartX + col * (cellW + 4);
                    int cy = gridStartY + row * (cellH + 2);
                    if (mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH) {
                        ModConfig config = ModConfig.getConfig();
                        config.posX = PRESET_X[row][col];
                        config.posY = PRESET_Y[row][col];
                        config.save();
                        selectedPresetRow = row;
                        selectedPresetCol = col;
                        return true;
                    }
                }
            }
        }

        ModConfig config = ModConfig.getConfig();
        int hudWidth = CpsHudOverlay.getHudWidth(config);
        int hudHeight = CpsHudOverlay.getHudHeight();
        int hudX = (int) (config.posX * (width - hudWidth));
        int hudY = (int) (config.posY * (height - hudHeight));
        hudX = Math.max(0, Math.min(hudX, width - hudWidth));
        hudY = Math.max(0, Math.min(hudY, height - hudHeight));

        if (mouseX >= hudX - 2 && mouseX <= hudX + hudWidth + 2 && mouseY >= hudY - 2 && mouseY <= hudY + hudHeight + 2) {
            dragging = true;
            dragOffsetX = mouseX - hudX;
            dragOffsetY = mouseY - hudY;
            selectedPresetRow = -1;
            selectedPresetCol = -1;
            return true;
        }

        return super.mouseClicked(click, always);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (dragging) {
            double mouseX = click.x();
            double mouseY = click.y();

            ModConfig config = ModConfig.getConfig();
            int hudWidth = CpsHudOverlay.getHudWidth(config);
            int hudHeight = CpsHudOverlay.getHudHeight();

            int newHudX = (int) (mouseX - dragOffsetX);
            int newHudY = (int) (mouseY - dragOffsetY);

            newHudX = Math.max(0, Math.min(newHudX, width - hudWidth));
            newHudY = Math.max(0, Math.min(newHudY, height - hudHeight));

            config.posX = (width - hudWidth) > 0 ? (double) newHudX / (width - hudWidth) : 0.0;
            config.posY = (height - hudHeight) > 0 ? (double) newHudY / (height - hudHeight) : 0.0;

            config.posX = Math.max(0.0, Math.min(1.0, config.posX));
            config.posY = Math.max(0.0, Math.min(1.0, config.posY));

            detectSelectedPreset();
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging) {
            dragging = false;
            ModConfig.getConfig().save();
            detectSelectedPreset();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        ModConfig.getConfig().save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
