package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.config.CounterEntry;
import com.notnawfas.ultimatepvputils.cps.CpsVariable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

import java.util.List;

public class CpsHudOverlay implements HudRenderCallback {

    private static final int INNER_PADDING = 4;
    private static final int BORDER_WIDTH = 1;
    private static final int SNAP_THRESHOLD = 6;

    private static boolean snappingActive = false;

    public static void setSnappingActive(boolean active) {
        snappingActive = active;
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
        String[] resolvedTexts = new String[count];
        int[] xs = new int[count];
        int[] ys = new int[count];
        int[] ws = new int[count];
        int[] hs = new int[count];

        for (int i = 0; i < count; i++) {
            CounterEntry entry = counters.get(i);
            String resolved = CpsVariable.resolve(entry.displayFormat);
            resolvedTexts[i] = resolved;
            float scale = (float) Math.max(0.5, Math.min(2.0, entry.scale));
            int textWidth = client.textRenderer.getWidth(resolved);
            int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
            int totalHeight = client.textRenderer.fontHeight + (INNER_PADDING + BORDER_WIDTH) * 2;
            int scaledWidth = (int) (totalWidth * scale);
            int scaledHeight = (int) (totalHeight * scale);

            ws[i] = scaledWidth;
            hs[i] = scaledHeight;
            xs[i] = Math.max(0, Math.min((int) (entry.posX * (screenW - scaledWidth)), screenW - scaledWidth));
            ys[i] = Math.max(0, Math.min((int) (entry.posY * (screenH - scaledHeight)), screenH - scaledHeight));
        }

        if (snappingActive) {
            for (int pass = 0; pass < 3; pass++) {
                for (int i = 0; i < count; i++) {
                    int bestDx = xs[i], bestDy = ys[i];
                    double bestDistX = SNAP_THRESHOLD + 1, bestDistY = SNAP_THRESHOLD + 1;

                    for (int j = 0; j < count; j++) {
                        if (i == j) continue;
                        int[] snapResult = findClosestSnap(xs[i], ys[i], ws[i], hs[i], xs[j], ys[j], ws[j], hs[j]);
                        if (snapResult[2] < bestDistX) { bestDistX = snapResult[2]; bestDx = snapResult[0]; }
                        if (snapResult[3] < bestDistY) { bestDistY = snapResult[3]; bestDy = snapResult[1]; }
                    }

                    if (Math.abs(xs[i]) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistX) bestDx = 0;
                    if (Math.abs(xs[i] + ws[i] - screenW) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistX) bestDx = screenW - ws[i];
                    if (Math.abs(ys[i]) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistY) bestDy = 0;
                    if (Math.abs(ys[i] + hs[i] - screenH) < SNAP_THRESHOLD && SNAP_THRESHOLD < bestDistY) bestDy = screenH - hs[i];

                    xs[i] = Math.max(0, Math.min(bestDx, screenW - ws[i]));
                    ys[i] = Math.max(0, Math.min(bestDy, screenH - hs[i]));
                }
            }
        }

        for (int i = 0; i < count; i++) {
            CounterEntry entry = counters.get(i);
            String resolved = resolvedTexts[i];
            Text displayText = Text.literal(resolved);
            float scale = (float) Math.max(0.5, Math.min(2.0, entry.scale));
            int textWidth = client.textRenderer.getWidth(resolved);
            int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
            int totalHeight = client.textRenderer.fontHeight + (INNER_PADDING + BORDER_WIDTH) * 2;

            drawContext.getMatrices().pushMatrix();
            try {
                drawContext.getMatrices().translate(xs[i], ys[i]);
                drawContext.getMatrices().scale(scale, scale);

                if (entry.showBackground) {
                    int borderColor = deriveBorderColor(entry.backgroundColor);
                    drawContext.fill(0, 0, totalWidth, totalHeight, borderColor);
                    drawContext.fill(BORDER_WIDTH, BORDER_WIDTH, totalWidth - BORDER_WIDTH, totalHeight - BORDER_WIDTH, entry.backgroundColor);
                }

                drawContext.drawText(client.textRenderer, displayText, INNER_PADDING + BORDER_WIDTH, INNER_PADDING + BORDER_WIDTH, entry.textColor, true);
            } finally {
                drawContext.getMatrices().popMatrix();
            }
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

    public static void renderPreview(DrawContext drawContext, CounterEntry entry, int forcedX, int forcedY) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(entry.displayFormat);
        Text displayText = Text.literal(resolved);
        float scale = (float) Math.max(0.5, Math.min(2.0, entry.scale));
        int textWidth = client.textRenderer.getWidth(resolved);
        int totalWidth = textWidth + (INNER_PADDING + BORDER_WIDTH) * 2;
        int totalHeight = client.textRenderer.fontHeight + (INNER_PADDING + BORDER_WIDTH) * 2;

        drawContext.getMatrices().pushMatrix();
        try {
            drawContext.getMatrices().translate(forcedX, forcedY);
            drawContext.getMatrices().scale(scale, scale);

            if (entry.showBackground) {
                int borderColor = deriveBorderColor(entry.backgroundColor);
                drawContext.fill(0, 0, totalWidth, totalHeight, borderColor);
                drawContext.fill(BORDER_WIDTH, BORDER_WIDTH, totalWidth - BORDER_WIDTH, totalHeight - BORDER_WIDTH, entry.backgroundColor);
            }

            drawContext.drawText(client.textRenderer, displayText, INNER_PADDING + BORDER_WIDTH, INNER_PADDING + BORDER_WIDTH, entry.textColor, true);
        } finally {
            drawContext.getMatrices().popMatrix();
        }
    }

    public static int getHudWidth(CounterEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        String resolved = CpsVariable.resolve(entry.displayFormat);
        float scale = (float) Math.max(0.5, Math.min(2.0, entry.scale));
        int textWidth = client.textRenderer.getWidth(resolved);
        return (int) ((textWidth + (INNER_PADDING + BORDER_WIDTH) * 2) * scale);
    }

    public static int getHudHeight(CounterEntry entry) {
        float scale = (float) Math.max(0.5, Math.min(2.0, entry.scale));
        int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;
        return (int) ((textHeight + (INNER_PADDING + BORDER_WIDTH) * 2) * scale);
    }

    private static int deriveBorderColor(int bgColor) {
        int srcA = (bgColor >> 24) & 0xFF;
        if (srcA == 0) return 0;
        int a = Math.min(255, srcA + 40);
        int r = Math.min(255, ((bgColor >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((bgColor >> 8) & 0xFF) + 30);
        int b = Math.min(255, (bgColor & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
