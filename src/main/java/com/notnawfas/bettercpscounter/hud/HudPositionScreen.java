package com.notnawfas.bettercpscounter.hud;

import com.notnawfas.bettercpscounter.config.CounterEntry;
import com.notnawfas.bettercpscounter.config.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudPositionScreen extends Screen {

    private static final int V_SIZE = 12;
    private static final int V_HIT_RADIUS = 14;
    private static final int DEADZONE = 3;

    private final Screen parent;
    private final CounterEntry counter;

    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private boolean resizing = false;
    private double resizeStartX = 0;
    private double resizeStartY = 0;
    private double resizeStartScale = 1.0;
    private double resizeStartHudW = 0;
    private double resizeStartHudH = 0;

    public HudPositionScreen(Screen parent, CounterEntry counter) {
        super(Text.literal("Position"));
        this.parent = parent;
        this.counter = counter;
    }

    @Override
    public void onDisplayed() {
        CpsHudOverlay.setSnappingActive(true);
    }

    @Override
    public void removed() {
        CpsHudOverlay.setSnappingActive(false);
        ModConfig.getConfig().save();
    }

    private CpsHudOverlay.ResolvedCounter rc() {
        return CpsHudOverlay.resolve(counter);
    }

    private int[] getHudBounds() {
        CpsHudOverlay.ResolvedCounter rc = rc();
        int hudX = Math.max(0, Math.min((int) (counter.posX * (width - rc.w())), width - rc.w()));
        int hudY = Math.max(0, Math.min((int) (counter.posY * (height - rc.h())), height - rc.h()));
        return new int[]{hudX, hudY, rc.w(), rc.h()};
    }

    private int[] getVCenter(int hudX, int hudY) {
        return new int[]{hudX + V_SIZE / 2 + 3, hudY + V_SIZE / 2 + 3};
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            this.client.gameRenderer.renderWorld(this.client.getRenderTickCounter());
        }

        if (counter == null) {
            drawContext.fill(0, 0, width, height, DrawUtils.OVERLAY_MED);
            drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, height / 2, 0xFFFFFF);
            return;
        }

        drawContext.fill(0, 0, width, height, DrawUtils.OVERLAY_LIGHT);

        int[] b = getHudBounds();
        int hudX = b[0], hudY = b[1], hudW = b[2], hudH = b[3];

        CpsHudOverlay.drawCounter(drawContext, rc(), hudX, hudY);

        long time = System.currentTimeMillis();
        float dashPhase = (time % 2000) / 2000.0f;
        int bx = hudX - 2, by = hudY - 2, bw = hudW + 4, bh = hudH + 4;
        drawAnimatedBorder(drawContext, bx, by, bw, bh, DrawUtils.KNOB, dashPhase);

        drawVHandle(drawContext, hudX, hudY, mouseX, mouseY);

        drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, height / 2, DrawUtils.KNOB);
    }

    private void drawVHandle(DrawContext dc, int hudX, int hudY, int mx, int my) {
        int[] vc = getVCenter(hudX, hudY);
        int cx = vc[0], cy = vc[1];
        boolean hovered = isVHit(mx, my, hudX, hudY);
        int color = (hovered || resizing) ? DrawUtils.ACCENT : DrawUtils.KNOB;

        // Draw a 135-degree V in the top-left corner
        // Two lines meeting at a point, forming a resize grip
        // The V opens toward the bottom-right (135deg between the two legs)
        // Top-left point of the V
        int px = cx - V_SIZE / 2;
        int py = cy - V_SIZE / 2;
        // Bottom vertex (tip points top-left, legs go bottom-right)
        // Line 1: from tip going right
        // Line 2: from tip going down
        // Draw thick lines for visibility
        for (int i = 0; i < 2; i++) {
            // Horizontal leg (going right from tip)
            dc.fill(px + i, py + i, px + V_SIZE, py + i + 1, color);
            // Vertical leg (going down from tip)
            dc.fill(px + i, py + i, px + i + 1, py + V_SIZE, color);
        }

        // Shadow/outline for contrast
        int shadow = 0x80000000;
        for (int i = 0; i < 2; i++) {
            dc.fill(px + i + 1, py + i + 1, px + V_SIZE + 1, py + i + 2, shadow);
            dc.fill(px + i + 1, py + i + 1, px + i + 2, py + V_SIZE + 1, shadow);
        }
    }

    private boolean isVHit(int mx, int my, int hudX, int hudY) {
        int[] vc = getVCenter(hudX, hudY);
        int dx = mx - vc[0], dy = my - vc[1];
        return dx * dx + dy * dy <= V_HIT_RADIUS * V_HIT_RADIUS;
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
        int[] b = getHudBounds();
        int hudX = b[0], hudY = b[1], hudW = b[2], hudH = b[3];

        // Check V handle hit first
        if (isVHit((int) mx, (int) my, hudX, hudY)) {
            resizing = true;
            resizeStartX = mx;
            resizeStartY = my;
            resizeStartScale = counter.scale;
            resizeStartHudW = hudW;
            resizeStartHudH = hudH;
            return true;
        }

        // Check drag on the HUD body
        if (mx >= hudX - 2 && mx <= hudX + hudW + 2 && my >= hudY - 2 && my <= hudY + hudH + 2) {
            dragging = true;
            dragOffsetX = mx - hudX;
            dragOffsetY = my - hudY;
            return true;
        }

        return super.mouseClicked(click, always);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (counter == null) return super.mouseDragged(click, dragX, dragY);

        double mx = click.x(), my = click.y();

        if (resizing) {
            double dx = mx - resizeStartX;
            double dy = my - resizeStartY;

            // Moving cursor down-right from V increases size, up-left decreases
            // Average the diagonal movement for uniform scaling
            double diagDelta = (dx + dy) / 2.0;

            float oldScale = (float) Math.max(0.5, Math.min(2.0, resizeStartScale));
            double baseW = resizeStartHudW / oldScale;
            double baseH = resizeStartHudH / oldScale;
            double avgBase = (baseW + baseH) / 2.0;
            if (avgBase <= 0) avgBase = 35;

            double newScale = oldScale + diagDelta / avgBase;
            newScale = Math.round(newScale * 10.0) / 10.0;
            counter.scale = Math.max(0.5, Math.min(2.0, newScale));
            return true;
        }

        if (dragging) {
            CpsHudOverlay.ResolvedCounter rc = rc();
            int newHudX = Math.max(0, Math.min((int) (mx - dragOffsetX), width - rc.w()));
            int newHudY = Math.max(0, Math.min((int) (my - dragOffsetY), height - rc.h()));
            counter.posX = (width - rc.w()) > 0 ? (double) newHudX / (width - rc.w()) : 0.0;
            counter.posY = (height - rc.h()) > 0 ? (double) newHudY / (height - rc.h()) : 0.0;
            counter.posX = Math.max(0.0, Math.min(1.0, counter.posX));
            counter.posY = Math.max(0.0, Math.min(1.0, counter.posY));
            counter.presetName = CounterEntry.detectPreset(counter.posX, counter.posY);
            return true;
        }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging) {
            dragging = false;
            ModConfig.getConfig().save();
            return true;
        }
        if (resizing) {
            resizing = false;
            ModConfig.getConfig().save();
            return true;
        }
        return super.mouseReleased(click);
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
