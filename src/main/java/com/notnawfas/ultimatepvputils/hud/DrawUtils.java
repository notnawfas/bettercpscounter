package com.notnawfas.ultimatepvputils.hud;

import net.minecraft.client.gui.DrawContext;

public final class DrawUtils {

    public static final int ACCENT = 0xFF4FC3F7;
    public static final int ACCENT_DIM = 0x804FC3F7;
    public static final int PANEL_BG     = 0xE0202020;
    public static final int CARD_BG      = 0xFF1E1E1E;
    public static final int FIELD_BG     = 0xFF1A1A1A;
    public static final int BORDER       = 0xFF444444;
    public static final int BORDER_LIGHT = 0xFF333333;
    public static final int BORDER_DIM   = 0xFF555555;
    public static final int TEXT_PRIMARY  = 0xFFE0E0E0;
    public static final int TEXT_LABEL   = 0xFFCCCCCC;
    public static final int TEXT_DIM = 0xFF888888;
    public static final int BTN_BG = 0xFF2A2A2A;
    public static final int BTN_HOVER    = 0xFF3A3A3A;
    public static final int TOGGLE_OFF   = 0xFF555555;
    public static final int OVERLAY_BG   = 0xC0101010;
    public static final int ADD_GREEN    = 0xFF4CAF50;
    public static final int ADD_BG       = 0xFF1E3A1E;
    public static final int ADD_HOVER    = 0xFF2A4A2A;
    public static final int DELETE_IDLE  = 0xFF666666;
    public static final int DELETE_HOVER = 0xFFFF4444;
    public static final int DELETE_WARN = 0xFFFF8800;
    public static final int RESET_BG = 0xFF2A1A1A;
    public static final int RESET_HOVER = 0xFF3A2222;
    public static final int RESET_BORDER = 0xFF663333;
    public static final int RESET_TEXT = 0xFFCC6666;
    public static final int DELETE_CONFIRM_BG = 0xFF2A2200;
    public static final int ARROW_IDLE = 0xFFAAAAAA;
    public static final int KNOB = 0xFFFFFFFF;
    public static final int KNOB_ALT = 0xFFDDDDDD;
    public static final int CURSOR_COLOR = 0xFFE0E0E0;
    public static final int OVERLAY_LIGHT = 0x30000000;
    public static final int OVERLAY_MED = 0x60000000;
    public static final int OVERLAY_DARK = 0xB0000000;
    public static final int HOVER_TINT = 0x20FFFFFF;
    public static final int HOVER_TINT_LIGHT = 0x10FFFFFF;

    private DrawUtils() {}

    public static void drawBorder(DrawContext dc, int x, int y, int w, int h, int color) {
        dc.fill(x, y, x + w, y + 1, color);
        dc.fill(x, y + h - 1, x + w, y + h, color);
        dc.fill(x, y, x + 1, y + h, color);
        dc.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawCheckerboard(DrawContext dc, int x, int y, int w, int h) {
        int cell = 4;
        for (int cx = 0; cx < w; cx += cell) {
            for (int cy = 0; cy < h; cy += cell) {
                boolean light = ((cx / cell) + (cy / cell)) % 2 == 0;
                dc.fill(x + cx, y + cy, x + Math.min(cx + cell, w), y + Math.min(cy + cell, h),
                        light ? 0xFFFFFFFF : 0xFFCCCCCC);
            }
        }
    }
}
