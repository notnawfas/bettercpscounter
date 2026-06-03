package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.CounterEntry;
import com.notnawfas.ultimatepvputils.config.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudPositionScreen extends Screen {

    private final Screen parent;
    private final CounterEntry counter;

    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private boolean resizing = false;
    private double resizeAnchorX = 0;
    private double resizeAnchorY = 0;
    private double resizeStartScale = 1.0;

    private static final int EDGE_ZONE = 8;

    public HudPositionScreen(Screen parent, CounterEntry counter) {
        super(Text.literal("Position"));
        this.parent = parent;
        this.counter = counter;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            this.client.gameRenderer.renderWorld(this.client.getRenderTickCounter());
        }

        if (counter == null) {
            drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, height / 2, 0xFFFFFF);
            return;
        }

        int hudWidth = CpsHudOverlay.getHudWidth(counter);
        int hudHeight = CpsHudOverlay.getHudHeight(counter);

        int hudX = (int) (counter.posX * (width - hudWidth));
        int hudY = (int) (counter.posY * (height - hudHeight));
        hudX = Math.max(0, Math.min(hudX, width - hudWidth));
        hudY = Math.max(0, Math.min(hudY, height - hudHeight));

        CpsHudOverlay.renderPreview(drawContext, counter, hudX, hudY);

        long time = System.currentTimeMillis();
        float dashPhase = (time % 2000) / 2000.0f;
        drawAnimatedBorder(drawContext, hudX - 2, hudY - 2, hudWidth + 4, hudHeight + 4, 0xFFFFFFFF, dashPhase);

        boolean onRightEdge = mouseX >= hudX + hudWidth - EDGE_ZONE && mouseX <= hudX + hudWidth + 2;
        boolean onBottomEdge = mouseY >= hudY + hudHeight - EDGE_ZONE && mouseY <= hudY + hudHeight + 2;
        boolean onCorner = onRightEdge && onBottomEdge;

        if (onCorner) {
            drawContext.fill(hudX + hudWidth - 8, hudY + hudHeight, hudX + hudWidth, hudY + hudHeight + 2, 0xFF4FC3F7);
            drawContext.fill(hudX + hudWidth, hudY + hudHeight - 8, hudX + hudWidth + 2, hudY + hudHeight, 0xFF4FC3F7);
        } else if (onRightEdge || onBottomEdge) {
            drawContext.fill(hudX + hudWidth - EDGE_ZONE, hudY + hudHeight, hudX + hudWidth + 2, hudY + hudHeight + 2, 0xFF4FC3F7);
            drawContext.fill(hudX + hudWidth, hudY + hudHeight - EDGE_ZONE, hudX + hudWidth + 2, hudY + hudHeight + 2, 0xFF4FC3F7);
        }

        drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, height / 2, 0xFFFFFF);
    }

    @Override
    public void onDisplayed() {
        CpsHudOverlay.setSnappingActive(true);
    }

    private void drawAnimatedBorder(DrawContext dc, int x, int y, int w, int h, int color, float phase) {
        int dashLen = 6, gapLen = 4, period = dashLen + gapLen;
        int offset = (int) (phase * period * 4);
        drawDashedLineH(dc, x, y, w, color, dashLen, gapLen, offset);
        drawDashedLineH(dc, x, y + h - 1, w, color, dashLen, gapLen, offset);
        drawDashedLineV(dc, x, y, h, color, dashLen, gapLen, offset);
        drawDashedLineV(dc, x + w - 1, y, h, color, dashLen, gapLen, offset);
    }

    private void drawDashedLineH(DrawContext dc, int x, int y, int w, int color, int dashLen, int gapLen, int offset) {
        int period = dashLen + gapLen;
        int pos = -offset % period;
        if (pos < 0) pos += period;
        while (pos < w) {
            int startX = x + pos;
            int endX = Math.min(x + pos + dashLen, x + w);
            if (startX < x + w) dc.fill(startX, y, endX, y + 1, color);
            pos += period;
        }
    }

    private void drawDashedLineV(DrawContext dc, int x, int y, int h, int color, int dashLen, int gapLen, int offset) {
        int period = dashLen + gapLen;
        int pos = -offset % period;
        if (pos < 0) pos += period;
        while (pos < h) {
            int startY = y + pos;
            int endY = Math.min(y + pos + dashLen, y + h);
            if (startY < y + h) dc.fill(x, startY, x + 1, endY, color);
            pos += period;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, always);
        if (counter == null) return true;

        double mx = click.x(), my = click.y();
        int hudWidth = CpsHudOverlay.getHudWidth(counter);
        int hudHeight = CpsHudOverlay.getHudHeight(counter);
        int hudX = Math.max(0, Math.min((int) (counter.posX * (width - hudWidth)), width - hudWidth));
        int hudY = Math.max(0, Math.min((int) (counter.posY * (height - hudHeight)), height - hudHeight));

        boolean onRightEdge = mx >= hudX + hudWidth - EDGE_ZONE && mx <= hudX + hudWidth + 2 && my >= hudY - 2 && my <= hudY + hudHeight + 2;
        boolean onBottomEdge = my >= hudY + hudHeight - EDGE_ZONE && my <= hudY + hudHeight + 2 && mx >= hudX - 2 && mx <= hudX + hudWidth + 2;

        if (onRightEdge || onBottomEdge) {
            resizing = true; resizeAnchorX = hudX; resizeAnchorY = hudY; resizeStartScale = counter.scale; return true;
        }

        if (mx >= hudX - 2 && mx <= hudX + hudWidth + 2 && my >= hudY - 2 && my <= hudY + hudHeight + 2) {
            dragging = true; dragOffsetX = mx - hudX; dragOffsetY = my - hudY; return true;
        }

        return super.mouseClicked(click, always);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (counter == null) return super.mouseDragged(click, dragX, dragY);
        double mx = click.x(), my = click.y();

        if (resizing) {
            float oldScale = (float) Math.max(0.5, Math.min(2.0, resizeStartScale));
            int oldWidth = CpsHudOverlay.getHudWidth(counter);
            int oldHeight = CpsHudOverlay.getHudHeight(counter);
            double dx = mx - resizeAnchorX, dy = my - resizeAnchorY;
            double baseWidth = oldWidth / oldScale, baseHeight = oldHeight / oldScale;
            if (baseWidth <= 0) baseWidth = 50;
            if (baseHeight <= 0) baseHeight = 20;
            double newScale = Math.max(dx / baseWidth, dy / baseHeight);
            newScale = Math.round(newScale * 10.0) / 10.0;
            counter.scale = Math.max(0.5, Math.min(2.0, newScale));
            return true;
        }

        if (dragging) {
            int hudWidth = CpsHudOverlay.getHudWidth(counter);
            int hudHeight = CpsHudOverlay.getHudHeight(counter);
            int newHudX = Math.max(0, Math.min((int) (mx - dragOffsetX), width - hudWidth));
            int newHudY = Math.max(0, Math.min((int) (my - dragOffsetY), height - hudHeight));
            counter.posX = (width - hudWidth) > 0 ? (double) newHudX / (width - hudWidth) : 0.0;
            counter.posY = (height - hudHeight) > 0 ? (double) newHudY / (height - hudHeight) : 0.0;
            counter.posX = Math.max(0.0, Math.min(1.0, counter.posX));
            counter.posY = Math.max(0.0, Math.min(1.0, counter.posY));
            counter.presetName = detectPresetName(counter.posX, counter.posY);
            return true;
        }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging) { dragging = false; ModConfig.getConfig().save(); return true; }
        if (resizing) { resizing = false; ModConfig.getConfig().save(); return true; }
        return super.mouseReleased(click);
    }

    private String detectPresetName(double posX, double posY) {
        if (Math.abs(posX) < 0.01 && Math.abs(posY) < 0.01) return "Top-Left";
        if (Math.abs(posX - 1.0) < 0.01 && Math.abs(posY) < 0.01) return "Top-Right";
        if (Math.abs(posX) < 0.01 && Math.abs(posY - 1.0) < 0.01) return "Bottom-Left";
        if (Math.abs(posX - 1.0) < 0.01 && Math.abs(posY - 1.0) < 0.01) return "Bottom-Right";
        return "Custom";
    }

    @Override
    public void close() {
        CpsHudOverlay.setSnappingActive(false);
        ModConfig.getConfig().save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
