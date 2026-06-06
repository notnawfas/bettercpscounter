package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.CounterEntry;
import com.notnawfas.ultimatepvputils.config.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudPositionScreen extends Screen {

    private enum Handle {
        NONE,
        TOP_LEFT, TOP, TOP_RIGHT,
        RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT,
        LEFT
    }

    private static final int DOT_RADIUS = 5;
    private static final int DOT_HIT_RADIUS = 8;
    private static final int DEADZONE = 5;

    private final Screen parent;
    private final CounterEntry counter;

    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private Handle activeHandle = Handle.NONE;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private double dragStartScreenX = 0;
    private double dragStartScreenY = 0;
    private double dragStartHudW = 0;
    private double dragStartHudH = 0;
    private double dragStartScale = 1.0;

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

        drawHandleDots(drawContext, hudX, hudY, hudW, hudH);

        Handle hovered = getHandleAt(mouseX, mouseY, hudX, hudY, hudW, hudH);
        if (hovered != Handle.NONE) {
            drawContext.drawCenteredTextWithShadow(textRenderer, getHandleTooltip(hovered), width / 2, height / 2 - 12, DrawUtils.ACCENT_DIM);
        }

        drawContext.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", width / 2, height / 2, DrawUtils.KNOB);
    }

    private void drawHandleDots(DrawContext dc, int hx, int hy, int hw, int hh) {
        int[][] dots = getDotPositions(hx, hy, hw, hh);
        for (int[] dot : dots) {
            int x = dot[0], y = dot[1];
            boolean isActive = dot == getActiveDot(hx, hy, hw, hh);
        int color = isActive ? DrawUtils.ACCENT : DrawUtils.KNOB;
        dc.fill(x - DOT_RADIUS, y - DOT_RADIUS, x + DOT_RADIUS, y + DOT_RADIUS, 0xFF000000);
            dc.fill(x - DOT_RADIUS + 1, y - DOT_RADIUS + 1, x + DOT_RADIUS - 1, y + DOT_RADIUS - 1, color);
        }
    }

    private int[][] getDotPositions(int hx, int hy, int hw, int hh) {
        int cx = hx + hw / 2, cy = hy + hh / 2;
        int r = hx + hw, b = hy + hh;
        return new int[][]{
            {hx, hy}, {cx, hy}, {r, hy},
            {r, cy},
            {r, b}, {cx, b}, {hx, b},
            {hx, cy}
        };
    }

    private int[] getActiveDot(int hx, int hy, int hw, int hh) {
        if (activeHandle == Handle.NONE) return null;
        int[][] dots = getDotPositions(hx, hy, hw, hh);
        int idx = activeHandle.ordinal() - 1;
        return idx >= 0 && idx < dots.length ? dots[idx] : null;
    }

    private String getHandleTooltip(Handle h) {
        return switch (h) {
            case TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT -> "Drag to resize";
            case TOP, BOTTOM -> "Drag to resize height";
            case LEFT, RIGHT -> "Drag to resize width";
            default -> "";
        };
    }

    private Handle getHandleAt(int mx, int my, int hx, int hy, int hw, int hh) {
        int[][] dots = getDotPositions(hx, hy, hw, hh);
        Handle[] handles = Handle.values();
        for (int i = 0; i < dots.length; i++) {
            int dx = mx - dots[i][0], dy = my - dots[i][1];
            if (dx * dx + dy * dy <= DOT_HIT_RADIUS * DOT_HIT_RADIUS) {
                return handles[i + 1];
            }
        }
        return Handle.NONE;
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

        Handle hit = getHandleAt((int) mx, (int) my, hudX, hudY, hudW, hudH);
        if (hit != Handle.NONE) {
            activeHandle = hit;
            dragStartX = mx;
            dragStartY = my;
            dragStartHudW = hudW;
            dragStartHudH = hudH;
            dragStartScale = counter.scale;
            dragStartScreenX = hudX;
            dragStartScreenY = hudY;
            return true;
        }

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

        if (activeHandle != Handle.NONE) {
            double rawDx = mx - dragStartX;
            double rawDy = my - dragStartY;
            if (Math.abs(rawDx) < DEADZONE) rawDx = 0;
            if (Math.abs(rawDy) < DEADZONE) rawDy = 0;

            float oldScale = (float) Math.max(0.5, Math.min(2.0, dragStartScale));
            double baseW = dragStartHudW / oldScale;
            double baseH = dragStartHudH / oldScale;
            if (baseW <= 0) baseW = 50;
            if (baseH <= 0) baseH = 20;

            double dx = rawDx;
            double dy = rawDy;

            boolean hasX = activeHandle == Handle.LEFT || activeHandle == Handle.RIGHT
                || activeHandle == Handle.TOP_LEFT || activeHandle == Handle.TOP_RIGHT
                || activeHandle == Handle.BOTTOM_LEFT || activeHandle == Handle.BOTTOM_RIGHT;
            boolean hasY = activeHandle == Handle.TOP || activeHandle == Handle.BOTTOM
                || activeHandle == Handle.TOP_LEFT || activeHandle == Handle.TOP_RIGHT
                || activeHandle == Handle.BOTTOM_LEFT || activeHandle == Handle.BOTTOM_RIGHT;

            if (hasX && !hasY) {
                dx = activeHandle == Handle.RIGHT ? dx : -dx;
                dy = 0;
            } else if (hasY && !hasX) {
                dy = activeHandle == Handle.BOTTOM ? dy : -dy;
                dx = 0;
            } else {
                dx = (activeHandle == Handle.RIGHT || activeHandle == Handle.BOTTOM_RIGHT || activeHandle == Handle.TOP_RIGHT) ? dx : -dx;
                dy = (activeHandle == Handle.BOTTOM || activeHandle == Handle.BOTTOM_RIGHT || activeHandle == Handle.BOTTOM_LEFT) ? dy : -dy;
            }

            double scaleDelta = (hasX && hasY) ? (dx + dy) / 2.0 : (hasX ? dx : dy);
            double avgBase = (baseW + baseH) / 2.0;
            double newScale = oldScale + scaleDelta / avgBase;
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
        if (activeHandle != Handle.NONE) {
            activeHandle = Handle.NONE;
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
