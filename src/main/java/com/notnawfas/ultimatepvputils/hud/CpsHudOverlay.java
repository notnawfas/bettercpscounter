package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.config.CounterEntry;
import com.notnawfas.ultimatepvputils.cps.CpsVariable;
import com.notnawfas.ultimatepvputils.cps.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

public class CpsHudOverlay implements HudRenderCallback {

    private static final int INNER_PADDING = 4;
    private static final int BORDER_WIDTH = 1;
    private static final int SNAP_THRESHOLD = 6;
    private static final long SNAP_TIMEOUT_MS = 30_000;

    private static boolean snappingActive = false;
    private static long snapActivatedAt = 0;

    public static void setSnappingActive(boolean active) {
        snappingActive = active;
        if (active) snapActivatedAt = System.currentTimeMillis();
    }

    private static final Map<String, String> resolveCache = new java.util.LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 32;
        }
    };

    public static String resolveCached(CounterEntry entry) {
        String format = entry.displayFormat;
        String cacheKey = format + "@" + System.currentTimeMillis() / 50;
        String cached = resolveCache.get(cacheKey);
        if (cached != null) return cached;
        String resolved = CpsVariable.resolve(format);
        resolveCache.put(cacheKey, resolved);
        return resolved;
    }

    public static int computeHudWidth(MinecraftClient client, String resolved, float scale) {
        int textWidth = client.textRenderer.getWidth(resolved);
        return (int) ((textWidth + (INNER_PADDING + BORDER_WIDTH) * 2) * scale);
    }

    public static int computeHudHeight(MinecraftClient client, float scale) {
        int totalHeight = client.textRenderer.fontHeight + (INNER_PADDING + BORDER_WIDTH) * 2;
        return (int) (totalHeight * scale);
    }

    public static ResolvedCounter resolve(CounterEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = resolveCached(entry);
        float scale = entry.clampedScale();
        int w = computeHudWidth(client, resolved, scale);
        int h = computeHudHeight(client, scale);
        return new ResolvedCounter(entry, resolved, scale, w, h);
    }

    public static void drawCounter(DrawContext dc, ResolvedCounter rc, int x, int y) {
        Text displayText = Text.literal(rc.resolved());
        int unscaledW = (rc.w() > 0 && rc.scale() > 0) ? (int)(rc.w() / rc.scale()) : 50;
        int unscaledH = (rc.h() > 0 && rc.scale() > 0) ? (int)(rc.h() / rc.scale()) : 20;

        dc.getMatrices().pushMatrix();
        try {
            dc.getMatrices().translate(x, y);
            dc.getMatrices().scale(rc.scale, rc.scale);

            if (rc.entry.showBackground) {
                int borderColor = deriveBorderColor(rc.entry.backgroundColor);
                dc.fill(0, 0, unscaledW, unscaledH, borderColor);
                dc.fill(BORDER_WIDTH, BORDER_WIDTH, unscaledW - BORDER_WIDTH, unscaledH - BORDER_WIDTH, rc.entry.backgroundColor);
            }

            dc.drawText(MinecraftClient.getInstance().textRenderer, displayText, INNER_PADDING + BORDER_WIDTH, INNER_PADDING + BORDER_WIDTH, rc.entry.textColor, true);
        } finally {
            dc.getMatrices().popMatrix();
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        ModConfig config = ModConfig.getConfig();
        if (!config.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<CounterEntry> counters = config.counters;
        if (counters.isEmpty()) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int count = counters.size();
        ResolvedCounter[] rcs = new ResolvedCounter[count];
        int[] xs = new int[count];
        int[] ys = new int[count];

        for (int i = 0; i < count; i++) {
            rcs[i] = resolve(counters.get(i));
            xs[i] = Math.max(0, Math.min((int) (rcs[i].entry.posX * (screenW - rcs[i].w())), screenW - rcs[i].w()));
            ys[i] = Math.max(0, Math.min((int) (rcs[i].entry.posY * (screenH - rcs[i].h())), screenH - rcs[i].h()));
        }

        if (snappingActive) {
            if (client.currentScreen instanceof HudPositionScreen) {
                snapActivatedAt = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - snapActivatedAt >= SNAP_TIMEOUT_MS
                || !(client.currentScreen instanceof HudPositionScreen)) {
                snappingActive = false;
            }
        }

        if (snappingActive) {
            for (int pass = 0; pass < 3; pass++) {
                for (int i = 0; i < count; i++) {
                    int bestDx = xs[i], bestDy = ys[i];
                    double bestDistX = SNAP_THRESHOLD + 1, bestDistY = SNAP_THRESHOLD + 1;

                    for (int j = 0; j < count; j++) {
                        if (i == j) continue;
                        int[] snapResult = findClosestSnap(xs[i], ys[i], rcs[i].w(), rcs[i].h(), xs[j], ys[j], rcs[j].w(), rcs[j].h());
                        if (snapResult[2] < bestDistX) { bestDistX = snapResult[2]; bestDx = snapResult[0]; }
                        if (snapResult[3] < bestDistY) { bestDistY = snapResult[3]; bestDy = snapResult[1]; }
                    }

                    if (Math.abs(xs[i]) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistX) bestDx = 0;
                    if (Math.abs(xs[i] + rcs[i].w() - screenW) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistX) bestDx = screenW - rcs[i].w();
                    if (Math.abs(ys[i]) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistY) bestDy = 0;
                    if (Math.abs(ys[i] + rcs[i].h() - screenH) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistY) bestDy = screenH - rcs[i].h();

                    xs[i] = Math.max(0, Math.min(bestDx, screenW - rcs[i].w()));
                    ys[i] = Math.max(0, Math.min(bestDy, screenH - rcs[i].h()));
                }
            }
        }

        for (int i = 0; i < count; i++) {
            drawCounter(drawContext, rcs[i], xs[i], ys[i]);
        }
    }

    private static int[] findClosestSnap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        int bestX = ax, bestY = ay;
        double bestDistX = SNAP_THRESHOLD + 1, bestDistY = SNAP_THRESHOLD + 1;

        int[][] xSnaps = {
            {bx, Math.abs(ax - bx)},
            {bx + bw, Math.abs(ax - bx - bw)},
            {bx + bw - aw, Math.abs(ax + aw - bx - bw)},
            {bx - aw, Math.abs(ax - bx + aw)}
        };
        for (int[] snap : xSnaps) {
            if (snap[1] < bestDistX) { bestDistX = snap[1]; bestX = snap[0]; }
        }

        int[][] ySnaps = {
            {by, Math.abs(ay - by)},
            {by + bh, Math.abs(ay - by - bh)},
            {by + bh - ah, Math.abs(ay + ah - by - bh)},
            {by - ah, Math.abs(ay - by + ah)}
        };
        for (int[] snap : ySnaps) {
            if (snap[1] < bestDistY) { bestDistY = snap[1]; bestY = snap[0]; }
        }

        return new int[]{bestX, bestY, (int) bestDistX, (int) bestDistY};
    }

    private static int deriveBorderColor(int bgColor) {
        int srcA = (bgColor >> 24) & 0xFF;
        if (srcA == 0) return 0;
        int a = Math.min(255, srcA + 40);
        int r = Math.max(0, ((bgColor >> 16) & 0xFF) - 50);
        int g = Math.max(0, ((bgColor >> 8) & 0xFF) - 50);
        int b = Math.max(0, (bgColor & 0xFF) - 50);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public record ResolvedCounter(CounterEntry entry, String resolved, float scale, int w, int h) {}
}
