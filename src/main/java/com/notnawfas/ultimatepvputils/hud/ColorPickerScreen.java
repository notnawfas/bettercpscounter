package com.notnawfas.ultimatepvputils.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {

    private static final int BAR_WIDTH = 150;
    private static final int BAR_HEIGHT = 14;
    private static final int PREVIEW_SIZE = 40;
    private static final int PADDING = 12;
    private static final int PANEL_H = 210;

    private final Screen parent;
    private final String titleText;
    private final IntConsumer onComplete;

    private int currentColor;

    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;
    private int alpha = 255;

    private boolean draggingSB = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    public ColorPickerScreen(Screen parent, String titleText, int initialColor, IntConsumer onComplete) {
        super(Text.literal(titleText));
        this.parent = parent;
        this.titleText = titleText;
        this.onComplete = onComplete;
        this.currentColor = initialColor;

        int r = (initialColor >> 16) & 0xFF;
        int g = (initialColor >> 8) & 0xFF;
        int b = initialColor & 0xFF;
        float[] hsb = rgbToHsb(r, g, b);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = (initialColor >> 24) & 0xFF;
    }

    @Override
    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        renderBackground(dc, mouseX, mouseY, delta);

        int panelW = BAR_WIDTH + PADDING * 2;
        int panelX = (width - panelW) / 2;
        int panelY = (height - PANEL_H) / 2;

        dc.fill(panelX, panelY, panelX + panelW, panelY + PANEL_H, 0xE0202020);
        dc.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF4FC3F7);
        dc.fill(panelX, panelY + PANEL_H - 1, panelX + panelW, panelY + PANEL_H, 0xFF444444);

        dc.drawCenteredTextWithShadow(textRenderer, titleText, width / 2, panelY + 6, 0xFFFFFF);

        int cy = panelY + 22;

        drawSBField(dc, panelX + PADDING, cy);
        cy += BAR_WIDTH + 8;

        drawHueBar(dc, panelX + PADDING, cy);
        cy += BAR_HEIGHT + 8;

        drawAlphaBar(dc, panelX + PADDING, cy);
        cy += BAR_HEIGHT + 8;

        currentColor = computeColor();
        int previewX = panelX + PADDING;

        drawCheckerboard(dc, previewX, cy, PREVIEW_SIZE, PREVIEW_SIZE);
        dc.fill(previewX, cy, previewX + PREVIEW_SIZE, cy + PREVIEW_SIZE, currentColor);
        drawBorder(dc, previewX, cy, PREVIEW_SIZE, PREVIEW_SIZE, 0xFF000000);

        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;
        int a = (currentColor >> 24) & 0xFF;
        String hex = String.format("#%02X%02X%02X%02X", a, r, g, b);
        dc.drawTextWithShadow(textRenderer, hex, previewX + PREVIEW_SIZE + 8, cy + PREVIEW_SIZE / 2 - 4, 0xFFCCCCCC);

        cy += PREVIEW_SIZE + 10;

        int btnW = (BAR_WIDTH - 4) / 2;
        int btnH = 20;
        drawTextButton(dc, "Cancel", previewX, cy, btnW, btnH, mouseX, mouseY);
        drawTextButton(dc, "Done", previewX + btnW + 4, cy, btnW, btnH, mouseX, mouseY);
    }

    @Override
    public void renderBackground(DrawContext dc, int mouseX, int mouseY, float delta) {
        dc.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void drawSBField(DrawContext dc, int x, int y) {
        int size = BAR_WIDTH;
        int step = 8;
        for (int px = 0; px < size; px += step) {
            for (int py = 0; py < size; py += step) {
                float s = (float) px / size;
                float b = 1f - (float) py / size;
                int rgb = hsbToRgb(hue, s, b);
                dc.fill(x + px, y + py, x + Math.min(px + step, size), y + Math.min(py + step, size), rgb | 0xFF000000);
            }
        }
        drawBorder(dc, x, y, size, size, 0x40000000);

        int knobX = x + (int) (saturation * size);
        int knobY = y + (int) ((1f - brightness) * size);
        dc.fill(knobX - 4, knobY - 1, knobX + 4, knobY + 2, 0xFFFFFFFF);
        dc.fill(knobX - 1, knobY - 4, knobX + 2, knobY + 4, 0xFFFFFFFF);
    }

    private void drawHueBar(DrawContext dc, int x, int y) {
        for (int px = 0; px < BAR_WIDTH; px++) {
            float h = (float) px / BAR_WIDTH;
            int rgb = hsbToRgb(h, 1f, 1f);
            dc.fill(x + px, y, x + px + 1, y + BAR_HEIGHT, rgb | 0xFF000000);
        }
        drawBorder(dc, x, y, BAR_WIDTH, BAR_HEIGHT, 0x40000000);

        int knobX = x + (int) (hue * BAR_WIDTH);
        dc.fill(knobX - 2, y - 2, knobX + 3, y + BAR_HEIGHT + 2, 0xFFFFFFFF);
        dc.fill(knobX - 1, y, knobX + 2, y + BAR_HEIGHT, hsbToRgb(hue, saturation, brightness) | 0xFF000000);
    }

    private void drawAlphaBar(DrawContext dc, int x, int y) {
        int fullColor = hsbToRgb(hue, saturation, brightness);
        for (int px = 0; px < BAR_WIDTH; px++) {
            float a = (float) px / BAR_WIDTH;
            drawCheckerboard(dc, x + px, y, 1, BAR_HEIGHT);
            int alphaVal = (int) (a * 255);
            int col = (alphaVal << 24) | (fullColor & 0x00FFFFFF);
            dc.fill(x + px, y, x + px + 1, y + BAR_HEIGHT, col);
        }
        drawBorder(dc, x, y, BAR_WIDTH, BAR_HEIGHT, 0x40000000);

        int knobX = x + (int) ((alpha / 255f) * BAR_WIDTH);
        dc.fill(knobX - 2, y - 2, knobX + 3, y + BAR_HEIGHT + 2, 0xFFFFFFFF);
        dc.fill(knobX - 1, y, knobX + 2, y + BAR_HEIGHT, currentColor);
    }

    private void drawCheckerboard(DrawContext dc, int x, int y, int w, int h) {
        int cell = 4;
        for (int cx = 0; cx < w; cx += cell) {
            for (int cy = 0; cy < h; cy += cell) {
                boolean light = ((cx / cell) + (cy / cell)) % 2 == 0;
                dc.fill(x + cx, y + cy, x + Math.min(cx + cell, w), y + Math.min(cy + cell, h), light ? 0xFFFFFFFF : 0xFFCCCCCC);
            }
        }
    }

    private void drawTextButton(DrawContext dc, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        dc.fill(x, y, x + w, y + h, hovered ? 0xFF3A3A3A : 0xFF2A2A2A);
        drawBorder(dc, x, y, w, h, 0xFF555555);
        dc.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFE0E0E0);
    }

    private void drawBorder(DrawContext dc, int x, int y, int w, int h, int color) {
        dc.fill(x, y, x + w, y + 1, color);
        dc.fill(x, y + h - 1, x + w, y + h, color);
        dc.fill(x, y, x + 1, y + h, color);
        dc.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int computeColor() {
        int rgb = hsbToRgb(hue, saturation, brightness);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private int panelX() { return (width - BAR_WIDTH - PADDING * 2) / 2; }
    private int panelY() { return (height - PANEL_H) / 2; }

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, always);
        int mx = (int) click.x(), my = (int) click.y();
        int px = panelX() + PADDING;
        int py = panelY() + 22;

        if (mx >= px && mx < px + BAR_WIDTH && my >= py && my < py + BAR_WIDTH) {
            draggingSB = true;
            saturation = Math.max(0f, Math.min(1f, (float) (mx - px) / BAR_WIDTH));
            brightness = Math.max(0f, Math.min(1f, 1f - (float) (my - py) / BAR_WIDTH));
            return true;
        }

        int hueY = py + BAR_WIDTH + 8;
        if (mx >= px && mx < px + BAR_WIDTH && my >= hueY && my < hueY + BAR_HEIGHT) {
            draggingHue = true;
            hue = Math.max(0f, Math.min(1f, (float) (mx - px) / BAR_WIDTH));
            return true;
        }

        int alphaY = hueY + BAR_HEIGHT + 8;
        if (mx >= px && mx < px + BAR_WIDTH && my >= alphaY && my < alphaY + BAR_HEIGHT) {
            draggingAlpha = true;
            alpha = (int) Math.max(0, Math.min(255, ((float) (mx - px) / BAR_WIDTH) * 255));
            return true;
        }

        int btnY = alphaY + BAR_HEIGHT + 8 + PREVIEW_SIZE + 10;
        int btnW = (BAR_WIDTH - 4) / 2;
        int btnH = 20;

        if (mx >= px && mx < px + btnW && my >= btnY && my < btnY + btnH) { close(); return true; }
        if (mx >= px + btnW + 4 && mx < px + BAR_WIDTH && my >= btnY && my < btnY + btnH) { onComplete.accept(computeColor()); close(); return true; }

        return super.mouseClicked(click, always);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        int mx = (int) click.x(), my = (int) click.y();
        int px = panelX() + PADDING;
        int py = panelY() + 22;

        if (draggingSB) {
            saturation = Math.max(0f, Math.min(1f, (float) (mx - px) / BAR_WIDTH));
            brightness = Math.max(0f, Math.min(1f, 1f - (float) (my - py) / BAR_WIDTH));
            return true;
        }
        if (draggingHue) { hue = Math.max(0f, Math.min(1f, (float) (mx - px) / BAR_WIDTH)); return true; }
        if (draggingAlpha) { alpha = (int) Math.max(0, Math.min(255, ((float) (mx - px) / BAR_WIDTH) * 255)); return true; }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingSB = false; draggingHue = false; draggingAlpha = false;
        return super.mouseReleased(click);
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
    @Override
    public boolean shouldPause() { return false; }

    // HSB/RGB conversion without java.awt.Color

    private static float[] rgbToHsb(int r, int g, int b) {
        float[] hsb = new float[3];
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        hsb[2] = max / 255f;
        hsb[1] = max == 0 ? 0 : delta / max;
        if (delta == 0) { hsb[0] = 0; }
        else if (max == r) { hsb[0] = ((g - b) / delta + (g < b ? 6 : 0)) / 6f; }
        else if (max == g) { hsb[0] = ((b - r) / delta + 2) / 6f; }
        else { hsb[0] = ((r - g) / delta + 4) / 6f; }
        return hsb;
    }

    private static int hsbToRgb(float h, float s, float b) {
        if (s == 0) { int v = (int) (b * 255); return (v << 16) | (v << 8) | v; }
        float hf = h * 6f;
        int i = (int) Math.floor(hf);
        float f = hf - i;
        float p = b * (1 - s);
        float q = b * (1 - s * f);
        float t = b * (1 - s * (1 - f));
        float r, g, bl;
        switch (i % 6) {
            case 0 -> { r = b; g = t; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = t; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = t; g = p; bl = b; }
            default -> { r = b; g = p; bl = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }
}
