package com.notnawfas.bettercpscounter.config;

import com.notnawfas.bettercpscounter.hud.ColorPickerScreen;
import com.notnawfas.bettercpscounter.hud.DrawUtils;
import com.notnawfas.bettercpscounter.hud.HudPositionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModConfigScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 120;
    private static final int PADDING = 12;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_SPACING = 6;
    private static final int CARD_PADDING = 8;
    private static final int COLLAPSED_HEIGHT = 28;
    private static final int MAX_FIELD_WIDTH = 400;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final float ANIM_SPEED = 0.18f;
    private static final float ANIM_THRESHOLD = 0.5f;

    private static final int LABEL_H = 14;
    private static final int FIELD_H = 20;
    private static final int FIELD_GAP = 4;
    private static final int TOGGLE_H = WIDGET_HEIGHT;
    private static final int SWATCH_H = WIDGET_HEIGHT;
    private static final int SLIDER_H = WIDGET_HEIGHT;
    private static final int DOCS_H = 18;
    private static final int PRESET_H = 22;
    private static final int EDIT_BTN_H = 22;
    private static final int RESET_BTN_H = 20;

    private static final int TAB_CPS = 0;

    private final Screen parent;
    private int expandedCardIndex = -1;

    private int focusedCardIndex = -1;
    private int focusedFieldIndex = -1;
    private final List<String> cardFieldTexts = new ArrayList<>();
    private final List<Integer> cardFieldCursors = new ArrayList<>();
    private final List<String> nameFieldTexts = new ArrayList<>();
    private final List<Integer> nameFieldCursors = new ArrayList<>();
    private final List<Integer> cardFieldScrolls = new ArrayList<>();
    private final List<Integer> nameFieldScrolls = new ArrayList<>();
    private long cursorBlinkStart = System.currentTimeMillis();

    private final Map<Integer, Float> cardAnimProgress = new HashMap<>();
    private long lastFrameTime = System.nanoTime();

    private boolean sliderDragging = false;
    private int sliderCardIndex = -1;
    private int sliderContentWidth = 0;

    private int scrollOffset = 0;

    private int pendingDeleteIndex = -1;
    private long pendingDeleteTime = 0;
    private int pendingDeleteVersion = -1;

    private int cursorType = 0; // 0 = default, 1 = hand
    private int lastCursorType = -1; // track last set cursor to avoid redundant updates

    public ModConfigScreen(Screen parent) {
        super(Text.literal("Better CPS Counter"));
        this.parent = parent;
        syncFieldTexts();
    }

    private void playClickSound() {
        if (this.client != null) {
            ClickableWidget.playClickSound(this.client.getSoundManager());
        }
    }

    private void syncFieldTexts() {
        List<CounterEntry> counters = ModConfig.getConfig().counters;
        while (cardFieldTexts.size() < counters.size()) { cardFieldTexts.add(""); cardFieldCursors.add(0); cardFieldScrolls.add(0); nameFieldTexts.add(""); nameFieldCursors.add(0); nameFieldScrolls.add(0); }
        while (cardFieldTexts.size() > counters.size()) { cardFieldTexts.remove(cardFieldTexts.size() - 1); cardFieldCursors.remove(cardFieldCursors.size() - 1); cardFieldScrolls.remove(cardFieldScrolls.size() - 1); nameFieldTexts.remove(nameFieldTexts.size() - 1); nameFieldCursors.remove(nameFieldCursors.size() - 1); nameFieldScrolls.remove(nameFieldScrolls.size() - 1); }
        for (int i = 0; i < counters.size(); i++) {
            if (focusedCardIndex != i || focusedFieldIndex != 0) cardFieldTexts.set(i, counters.get(i).displayFormat);
            if (cardFieldCursors.get(i) > cardFieldTexts.get(i).length()) cardFieldCursors.set(i, cardFieldTexts.get(i).length());
            if (focusedCardIndex != i || focusedFieldIndex != 1) nameFieldTexts.set(i, counters.get(i).name);
            if (nameFieldCursors.get(i) > nameFieldTexts.get(i).length()) nameFieldCursors.set(i, nameFieldTexts.get(i).length());
        }
    }

    private int getExpandedCardHeight() {
        return COLLAPSED_HEIGHT + CARD_PADDING
            + LABEL_H + FIELD_H + FIELD_GAP
            + LABEL_H + FIELD_H + FIELD_GAP
            + DOCS_H
            + TOGGLE_H + WIDGET_SPACING
            + LABEL_H + SWATCH_H + WIDGET_SPACING + FIELD_GAP
            + LABEL_H + SLIDER_H
            + WIDGET_SPACING + FIELD_GAP
            + LABEL_H + PRESET_H
            + WIDGET_SPACING + FIELD_GAP
            + EDIT_BTN_H + WIDGET_SPACING
            + RESET_BTN_H + WIDGET_SPACING
            + CARD_PADDING;
    }

    private float getCardProgress(int index) {
        Float p = cardAnimProgress.get(index);
        return p != null ? p : 0f;
    }

    private int getAnimatedCardHeight(int index) {
        float t = getCardProgress(index);
        if (t <= 0f) return COLLAPSED_HEIGHT;
        if (t >= 1f) return getExpandedCardHeight();
        return (int) (COLLAPSED_HEIGHT + (getExpandedCardHeight() - COLLAPSED_HEIGHT) * t);
    }

    private void tickAnimations() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrameTime) / 1_000_000_000f, 0.1f);
        lastFrameTime = now;

        for (int i = 0; i < ModConfig.getConfig().counters.size(); i++) {
            float current = getCardProgress(i);
            float target = (i == expandedCardIndex) ? 1f : 0f;
            if (Math.abs(current - target) < 0.005f) {
                cardAnimProgress.put(i, target);
            } else {
                float next = current + (target - current) * Math.min(ANIM_SPEED * 60f * dt, 0.999f);
                next = Math.max(0f, Math.min(1f, next));
                cardAnimProgress.put(i, next);
            }
        }

        cardAnimProgress.keySet().removeIf(i -> i >= ModConfig.getConfig().counters.size());
    }

    private int getTotalContentHeight() {
        List<CounterEntry> counters = ModConfig.getConfig().counters;
        if (counters.isEmpty()) return 32 + 40 + 22 + WIDGET_SPACING;
        int h = 32;
        for (int i = 0; i < counters.size(); i++) {
            h += getAnimatedCardHeight(i) + WIDGET_SPACING;
        }
        h += 4 + 22;
        return h;
    }

    private long lastSaveFlush = System.currentTimeMillis();

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - lastSaveFlush > 5000) {
            lastSaveFlush = now;
            ModConfig.getConfig().flushIfDirty();
        }
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float delta) {
        tickAnimations();
        renderBackground(dc, mx, my, delta);
        drawSidebar(dc, mx, my);

        int contentX = SIDEBAR_WIDTH + PADDING;
        int contentW = width - SIDEBAR_WIDTH - PADDING * 2;

        drawCpsTab(dc, ModConfig.getConfig(), contentX, contentW, mx, my);

        dc.drawCenteredTextWithShadow(textRenderer, title, SIDEBAR_WIDTH + (width - SIDEBAR_WIDTH) / 2, 8, 0xFFFFFF);

        // Apply cursor based on hover state (only when changed to avoid flicker)
        if (this.client != null && this.client.getWindow() != null && cursorType != lastCursorType) {
            lastCursorType = cursorType;
            switch (cursorType) {
                case 1 -> this.client.getWindow().setCursor(Cursor.createStandard(GLFW.GLFW_HAND_CURSOR, "hand", Cursor.DEFAULT));
                default -> this.client.getWindow().setCursor(Cursor.DEFAULT);
            }
        }
        cursorType = 0; // Reset for next frame
    }

    @Override
    public void renderBackground(DrawContext dc, int mx, int my, float delta) {
        dc.fill(0, 0, this.width, this.height, DrawUtils.OVERLAY_BG);
    }

    private void drawSidebar(DrawContext dc, int mx, int my) {
        int x = 0, y = 24;
        dc.fill(x, y, x + SIDEBAR_WIDTH, height, DrawUtils.OVERLAY_BG);

        ModConfig config = ModConfig.getConfig();
        drawToggleRow(dc, "HUD Enabled", config.enabled, x + 8, y + 4, SIDEBAR_WIDTH - 16, mx, my);
        y += WIDGET_HEIGHT + 12;

        dc.fill(x, y, x + SIDEBAR_WIDTH, y + 1, DrawUtils.HOVER_TINT);
        y += 8;

        boolean hovered = mx >= x && mx < x + SIDEBAR_WIDTH && my >= y && my < y + 28;
        if (hovered) {
            dc.fill(x, y, x + SIDEBAR_WIDTH, y + 28, DrawUtils.HOVER_TINT);
            cursorType = 1; // Hand cursor
        }
        dc.fill(x, y, x + 2, y + 28, DrawUtils.ACCENT);
        dc.drawTextWithShadow(textRenderer, "CPS Counter", x + 14, y + 9, DrawUtils.ACCENT);
    }

    // ========== CPS Tab ==========

    private void drawCpsTab(DrawContext dc, ModConfig config, int x, int w, int mx, int my) {
        int maxScroll = Math.max(0, getTotalContentHeight() - (height - 24));
        scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));

        int cy = 32 + scrollOffset;
        List<CounterEntry> counters = config.counters;

        if (counters.isEmpty()) {
            dc.drawCenteredTextWithShadow(textRenderer, "No Counters Found", x + w / 2, cy + 20, DrawUtils.TEXT_DIM);
            cy += 40;
            drawAddButton(dc, x, cy, w, mx, my);
            return;
        }

        for (int i = 0; i < counters.size(); i++) {
            CounterEntry entry = counters.get(i);
            int cardH = getAnimatedCardHeight(i);
            drawCard(dc, entry, i, x, cy, w, cardH, mx, my);
            cy += cardH + WIDGET_SPACING;
        }

        drawAddButton(dc, x, cy + 4, w, mx, my);
        drawScrollbar(dc, x, w, maxScroll);
    }

    private void drawScrollbar(DrawContext dc, int x, int w, int maxScroll) {
        if (maxScroll <= 0) return;
        int viewH = height - 24;
        int totalH = getTotalContentHeight();
        int trackY = 32;
        int trackH = viewH - 32;
        int thumbH = Math.max(20, (int)((float)viewH / totalH * trackH));
        int thumbY = trackY + (int)((float)(-scrollOffset) / maxScroll * (trackH - thumbH));
        int trackX = x + w - SCROLLBAR_WIDTH - 2;
        dc.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackH, DrawUtils.HOVER_TINT);
        dc.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, 0x80FFFFFF);
    }

    // ========== Card Rendering ==========

    private void drawCard(DrawContext dc, CounterEntry entry, int index, int x, int y, int w, int cardH, int mx, int my) {
        dc.fill(x, y, x + w, y + cardH, DrawUtils.CARD_BG);
        dc.fill(x, y, x + w, y + 1, DrawUtils.BORDER);
        dc.fill(x, y + cardH - 1, x + w, y + cardH, DrawUtils.BORDER_LIGHT);
        dc.fill(x, y, x + 1, y + cardH, DrawUtils.BORDER);
        dc.fill(x + w - 1, y, x + w, y + cardH, DrawUtils.BORDER);

        float progress = getCardProgress(index);
        boolean isExpanded = progress > ANIM_THRESHOLD;

        String arrow = isExpanded ? "\u25BC" : "\u25B6";
        boolean arrowHovered = mx >= x && mx < x + 24 && my >= y && my < y + COLLAPSED_HEIGHT;
        if (arrowHovered) cursorType = 1;
        dc.drawTextWithShadow(textRenderer, arrow, x + 6, y + 9, arrowHovered ? DrawUtils.ACCENT : DrawUtils.ARROW_IDLE);
        dc.drawTextWithShadow(textRenderer, entry.name, x + 24, y + 9, DrawUtils.TEXT_PRIMARY);

        int deleteX = x + w - 18;
        boolean deleteHovered = mx >= deleteX && mx < x + w && my >= y && my < y + COLLAPSED_HEIGHT;
        if (deleteHovered) cursorType = 1;

        if (pendingDeleteIndex == index && pendingDeleteVersion == ModConfig.getConfig().counters.size() && System.currentTimeMillis() - pendingDeleteTime < 3000) {
            dc.drawTextWithShadow(textRenderer, "\u2715", deleteX, y + 9, DrawUtils.DELETE_WARN);
            int confirmX = deleteX - 50;
            int confirmW = textRenderer.getWidth("Confirm?");
            dc.fill(confirmX - 3, y + 6, confirmX + confirmW + 3, y + 20, DrawUtils.DELETE_CONFIRM_BG);
            DrawUtils.drawBorder(dc, confirmX - 3, y + 6, confirmW + 6, 14, DrawUtils.DELETE_WARN);
            dc.drawTextWithShadow(textRenderer, "Confirm?", confirmX, y + 9, DrawUtils.DELETE_WARN);
        } else {
            dc.drawTextWithShadow(textRenderer, "\u2715", deleteX, y + 9, deleteHovered ? DrawUtils.DELETE_HOVER : DrawUtils.DELETE_IDLE);
        }

        if (progress <= 0f) return;

        int cx = x + CARD_PADDING;
        int cw = w - CARD_PADDING * 2;
        int visibleBodyH = cardH - COLLAPSED_HEIGHT;

        dc.enableScissor(x, y + COLLAPSED_HEIGHT, x + w, y + COLLAPSED_HEIGHT + visibleBodyH);

        int cy = y + COLLAPSED_HEIGHT + CARD_PADDING;

        dc.drawTextWithShadow(textRenderer, "Name:", cx, cy, DrawUtils.TEXT_LABEL);
        cy += LABEL_H;
        int fieldW = Math.min(cw, MAX_FIELD_WIDTH);
        boolean nameFocused = focusedCardIndex == index && focusedFieldIndex == 1;
        int nameCursor = nameFieldCursors.size() > index ? nameFieldCursors.get(index) : entry.name.length();
        drawTextField(dc, nameFieldTexts.size() > index ? nameFieldTexts.get(index) : entry.name, nameCursor, cx, cy, fieldW, nameFocused);
        cy += FIELD_H + FIELD_GAP;

        dc.drawTextWithShadow(textRenderer, "Display Format:", cx, cy, DrawUtils.TEXT_LABEL);
        cy += LABEL_H;
        boolean fmtFocused = focusedCardIndex == index && focusedFieldIndex == 0;
        int fmtCursor = cardFieldCursors.size() > index ? cardFieldCursors.get(index) : entry.displayFormat.length();
        drawTextField(dc, cardFieldTexts.size() > index ? cardFieldTexts.get(index) : entry.displayFormat, fmtCursor, cx, cy, fieldW, fmtFocused);
        cy += FIELD_H + FIELD_GAP;

        String docsText = "\u2139 Variables: modrinth.com/mod/better-cps-counter";
        dc.drawTextWithShadow(textRenderer, docsText, cx, cy, DrawUtils.ACCENT);
        int docsW = textRenderer.getWidth(docsText);
        boolean docsHovered = mx >= cx && mx < cx + docsW && my >= cy && my < cy + DOCS_H;
        dc.fill(cx, cy + 11, cx + docsW, cy + 12, docsHovered ? DrawUtils.ACCENT : DrawUtils.ACCENT_DIM);
        cy += DOCS_H;

        drawToggleRow(dc, "Show Background", entry.showBackground, cx, cy, cw, mx, my);
        cy += WIDGET_HEIGHT + WIDGET_SPACING;

        int halfW = (cw - 8) / 2;
        dc.drawTextWithShadow(textRenderer, "Background Color", cx, cy, DrawUtils.TEXT_LABEL);
        cy += LABEL_H;
        drawColorSwatch(dc, entry.backgroundColor, cx, cy, halfW, mx, my);
        dc.drawTextWithShadow(textRenderer, "Text Color", cx + halfW + 8, cy - LABEL_H, DrawUtils.TEXT_LABEL);
        drawColorSwatch(dc, entry.textColor, cx + halfW + 8, cy, halfW, mx, my);
        cy += SWATCH_H + WIDGET_SPACING + FIELD_GAP;

        dc.drawTextWithShadow(textRenderer, "Scale: " + String.format("%.1fx", entry.scale), cx, cy, DrawUtils.TEXT_LABEL);
        cy += LABEL_H;
        drawSlider(dc, entry.scale, cx, cy, cw, mx, my);
        cy += SLIDER_H + WIDGET_SPACING + FIELD_GAP;

        // Position preset
        cy += LABEL_H;
        int positionLabelW = textRenderer.getWidth("Position") + 8;
        dc.drawTextWithShadow(textRenderer, "Position", cx, cy + 7, DrawUtils.TEXT_LABEL);
        int presetBtnX = cx + positionLabelW;
        int presetW = Math.min(180, cx + cw - presetBtnX);
        if (presetW >= 60) {
            boolean presetHovered = mx >= presetBtnX && mx < presetBtnX + presetW && my >= cy && my < cy + PRESET_H;
            dc.fill(presetBtnX, cy, presetBtnX + presetW, cy + PRESET_H, presetHovered ? DrawUtils.BTN_HOVER : DrawUtils.BTN_BG);
            DrawUtils.drawBorder(dc, presetBtnX, cy, presetW, PRESET_H, DrawUtils.BORDER_DIM);
            dc.drawCenteredTextWithShadow(textRenderer, entry.presetName, presetBtnX + presetW / 2, cy + 7, DrawUtils.ACCENT);
        } else {
            dc.drawTextWithShadow(textRenderer, entry.presetName, cx, cy + 7, DrawUtils.ACCENT);
        }
        cy += PRESET_H + WIDGET_SPACING + FIELD_GAP;

        // Edit Custom Position button (always visible, same width as preset, grayed out when not Custom)
        boolean isCustom = entry.isCustomPosition();
        int editBtnX = presetBtnX;
        int editBtnW = presetW;
        boolean editHovered = isCustom && mx >= editBtnX && mx < editBtnX + editBtnW && my >= cy && my < cy + EDIT_BTN_H;
        int editBgColor = isCustom ? (editHovered ? DrawUtils.BTN_HOVER : DrawUtils.BTN_BG) : DrawUtils.BTN_DISABLED;
        int editBorderColor = isCustom ? DrawUtils.BORDER_DIM : DrawUtils.BORDER_DISABLED;
        int editTextColor = isCustom ? DrawUtils.TEXT_PRIMARY : DrawUtils.TEXT_DISABLED;
        dc.fill(editBtnX, cy, editBtnX + editBtnW, cy + EDIT_BTN_H, editBgColor);
        DrawUtils.drawBorder(dc, editBtnX, cy, editBtnW, EDIT_BTN_H, editBorderColor);
        dc.drawCenteredTextWithShadow(textRenderer, "Edit Custom Position", editBtnX + editBtnW / 2, cy + 7, editTextColor);
        cy += EDIT_BTN_H + WIDGET_SPACING + FIELD_GAP;

        boolean resetHovered = mx >= cx && mx < cx + cw && my >= cy && my < cy + RESET_BTN_H;
        dc.fill(cx, cy, cx + cw, cy + RESET_BTN_H, resetHovered ? DrawUtils.RESET_HOVER : DrawUtils.RESET_BG);
        DrawUtils.drawBorder(dc, cx, cy, cw, RESET_BTN_H, DrawUtils.RESET_BORDER);
        dc.drawCenteredTextWithShadow(textRenderer, "Reset to Defaults", cx + cw / 2, cy + 6, DrawUtils.RESET_TEXT);

        dc.disableScissor();
    }

    // ========== Shared Widget Helpers ==========

    private void drawToggleRow(DrawContext dc, String label, boolean value, int x, int y, int w, int mx, int my) {
        int toggleW = 40, toggleH = 16, toggleX = x + w - toggleW;
        boolean hovered = mx >= toggleX && mx < toggleX + toggleW && my >= y + 2 && my < y + 2 + toggleH;
        if (hovered) cursorType = 1;
        dc.drawTextWithShadow(textRenderer, label, x, y + 6, DrawUtils.TEXT_LABEL);
        dc.fill(toggleX, y + 2, toggleX + toggleW, y + 2 + toggleH, value ? DrawUtils.ACCENT : DrawUtils.TOGGLE_OFF);
        dc.fill(value ? toggleX + toggleW - 12 : toggleX + 2, y + 3, value ? toggleX + toggleW - 2 : toggleX + 12, y + 1 + toggleH, DrawUtils.KNOB);
        if (hovered) dc.fill(toggleX, y + 2, toggleX + toggleW, y + 2 + toggleH, DrawUtils.HOVER_TINT_LIGHT);
    }

    private void drawColorSwatch(DrawContext dc, int color, int x, int y, int w, int mx, int my) {
        int h = 20;
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        if (hovered) cursorType = 1;
        DrawUtils.drawCheckerboard(dc, x + 1, y + 1, w - 2, h - 2);
        dc.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        DrawUtils.drawBorder(dc, x, y, w, h, 0xFF000000);
        if (hovered) dc.fill(x, y, x + w, y + h, DrawUtils.HOVER_TINT);
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        String hex = String.format("#%02X%02X%02X%02X", a, r, g, b);
        dc.drawCenteredTextWithShadow(textRenderer, hex, x + w / 2, y + 6, (r * 0.299 + g * 0.587 + b * 0.114) > 128 && a > 128 ? 0xFF000000 : 0xFFFFFFFF);
    }

    private void drawSlider(DrawContext dc, double value, int x, int y, int w, int mx, int my) {
        dc.fill(x, y + 6, x + w, y + 14, DrawUtils.BORDER_LIGHT);
        double ratio = (value - 0.5) / 1.5;
        int filledW = (int) (w * ratio);
        dc.fill(x, y + 6, x + filledW, y + 14, DrawUtils.ACCENT);
        int knobCenter = Math.max(0, Math.min(w - 8, filledW));
        dc.fill(x + knobCenter - 4, y + 3, x + knobCenter + 4, y + 17, DrawUtils.KNOB);
        // Set hand cursor when hovering over slider track or knob
        boolean sliderHovered = mx >= x && mx < x + w && my >= y && my < y + 20;
        if (sliderHovered) cursorType = 1;
    }

    private void drawAddButton(DrawContext dc, int x, int y, int w, int mx, int my) {
        int btnH = 22;
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + btnH;
        if (hovered) cursorType = 1;
        dc.fill(x, y, x + w, y + btnH, hovered ? DrawUtils.ADD_HOVER : DrawUtils.ADD_BG);
        DrawUtils.drawBorder(dc, x, y, w, btnH, DrawUtils.ADD_GREEN);
        dc.drawCenteredTextWithShadow(textRenderer, "(+) Add New Counter", x + w / 2, y + 7, DrawUtils.ADD_GREEN);
    }

    // ========== Click Handling ==========

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != 0) return super.mouseClicked(click, always);
        int mx = (int) click.x(), my = (int) click.y();

        ModConfig config = ModConfig.getConfig();
        int toggleX = 8, toggleY = 28, toggleW = SIDEBAR_WIDTH - 16, toggleH = 20;
        if (mx >= toggleX && mx < toggleX + toggleW && my >= toggleY && my < toggleY + toggleH) {
            config.enabled = !config.enabled; config.requestSave(); playClickSound(); return true;
        }

        int contentX = SIDEBAR_WIDTH + PADDING;
        int contentWidth = width - SIDEBAR_WIDTH - PADDING * 2;

        handleCpsTabClick(mx, my, contentX, contentWidth, config);
        return true;
    }

    private void handleCpsTabClick(int mx, int my, int x, int w, ModConfig config) {
        int cy = 32 + scrollOffset;
        List<CounterEntry> counters = config.counters;

        if (counters.isEmpty()) {
            if (mx >= x && mx < x + w && my >= cy + 40 && my < cy + 62) {
                addCounter(config);
                playClickSound();
            }
            return;
        }

        for (int i = 0; i < counters.size(); i++) {
            int cardH = getAnimatedCardHeight(i);
            if (my >= cy && my < cy + cardH && mx >= x && mx < x + w) {
                if (my < cy + COLLAPSED_HEIGHT) {
                    boolean deleteHit = mx >= x + w - 18 || (pendingDeleteIndex == i && pendingDeleteVersion == counters.size() && System.currentTimeMillis() - pendingDeleteTime < 3000 && mx >= x + 24);
                    if (deleteHit) {
                        if (pendingDeleteIndex == i && pendingDeleteVersion == counters.size() && System.currentTimeMillis() - pendingDeleteTime < 3000) {
                            counters.remove(i);
                            if (expandedCardIndex == i) expandedCardIndex = -1;
                            else if (expandedCardIndex > i) expandedCardIndex--;
                            if (focusedCardIndex == i) { focusedCardIndex = -1; focusedFieldIndex = -1; }
                            else if (focusedCardIndex > i) focusedCardIndex--;
                            reindexAnimProgress(i);
                            pendingDeleteIndex = -1;
                            syncFieldTexts(); config.requestSave();
                            playClickSound();
                        } else {
                            pendingDeleteIndex = i;
                            pendingDeleteTime = System.currentTimeMillis();
                            pendingDeleteVersion = counters.size();
                            playClickSound();
                        }
                        return;
                    }
                    pendingDeleteIndex = -1;
                    expandedCardIndex = (expandedCardIndex == i) ? -1 : i;
                    if (expandedCardIndex == i) syncFieldTexts();
                    playClickSound();
                    return;
                }
                if (getCardProgress(i) > ANIM_THRESHOLD) {
                    handleExpandedCardClick(mx, my, x, cy + COLLAPSED_HEIGHT, w, i, counters.get(i), config);
                }
                return;
            }
            cy += cardH + WIDGET_SPACING;
        }

        pendingDeleteIndex = -1;

        int addBtnY = cy + 4;
        if (mx >= x && mx < x + w && my >= addBtnY && my < addBtnY + 22) {
            addCounter(config);
            playClickSound();
        }
    }

    private void addCounter(ModConfig config) {
        List<CounterEntry> counters = config.counters;
        counters.add(new CounterEntry("Counter " + (counters.size() + 1), "%cps_mouse.left%", true, 0x80000000, 0xFFFFFFFF, 1.0, 0.0, 0.0, "Top-Left"));
        pendingDeleteIndex = -1;
        syncFieldTexts(); config.requestSave(); autoScrollToLastCounter();
    }

    private void reindexAnimProgress(int removedIndex) {
        cardAnimProgress.remove(removedIndex);
        Map<Integer, Float> reindexed = new HashMap<>();
        for (Map.Entry<Integer, Float> entry : cardAnimProgress.entrySet()) {
            int key = entry.getKey();
            if (key > removedIndex) key--;
            reindexed.put(key, entry.getValue());
        }
        cardAnimProgress.clear();
        cardAnimProgress.putAll(reindexed);
    }

    private void autoScrollToLastCounter() {
        int totalH = getTotalContentHeight();
        int viewH = height - 24;
        int maxScroll = Math.max(0, totalH - viewH);
        scrollOffset = -maxScroll;
    }

    private void handleExpandedCardClick(int mx, int my, int cardX, int cardBodyY, int cardW, int index, CounterEntry entry, ModConfig config) {
        int cx = cardX + CARD_PADDING;
        int cw = cardW - CARD_PADDING * 2;
        int fieldW = Math.min(cw, MAX_FIELD_WIDTH);
        int cy = cardBodyY + CARD_PADDING;

        // Name field
        cy += LABEL_H;
        if (mx >= cx && mx < cx + fieldW && my >= cy && my < cy + FIELD_H) {
            focusedCardIndex = index; focusedFieldIndex = 1;
            setCursorFromClick(mx, cx, index, nameFieldTexts, nameFieldCursors);
            cursorBlinkStart = System.currentTimeMillis();
            return;
        }
        cy += FIELD_H + FIELD_GAP;

        // Display Format field
        cy += LABEL_H;
        if (mx >= cx && mx < cx + fieldW && my >= cy && my < cy + FIELD_H) {
            focusedCardIndex = index; focusedFieldIndex = 0;
            setCursorFromClick(mx, cx, index, cardFieldTexts, cardFieldCursors);
            cursorBlinkStart = System.currentTimeMillis();
            return;
        }
        cy += FIELD_H + FIELD_GAP;

        // Variables link
        String docsText = "\u2139 Variables: modrinth.com/mod/better-cps-counter";
        int docsW = textRenderer.getWidth(docsText);
        if (mx >= cx && mx < cx + docsW && my >= cy && my < cy + DOCS_H) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://modrinth.com/mod/better-cps-counter"));
                }
            } catch (Exception ignored) {}
            return;
        }
        cy += DOCS_H;

        // Show Background toggle
        int toggleW = 40, toggleH = 16, toggleX = cx + cw - toggleW;
        if (mx >= toggleX && mx < toggleX + toggleW && my >= cy + 2 && my < cy + 2 + toggleH) {
            entry.showBackground = !entry.showBackground;
            config.requestSave();
            playClickSound();
            return;
        }
        cy += WIDGET_HEIGHT + WIDGET_SPACING;

        // Color swatches
        int halfW = (cw - 8) / 2;
        cy += LABEL_H;
        if (mx >= cx && mx < cx + halfW && my >= cy && my < cy + SWATCH_H) {
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, "Background Color", entry.backgroundColor, c -> {
                entry.backgroundColor = c;
                ModConfig.getConfig().requestSave();
            }));
            playClickSound();
            return;
        }
        if (mx >= cx + halfW + 8 && mx < cx + halfW + 8 + halfW && my >= cy && my < cy + SWATCH_H) {
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, "Text Color", entry.textColor, c -> {
                entry.textColor = c;
                ModConfig.getConfig().requestSave();
            }));
            playClickSound();
            return;
        }
        cy += SWATCH_H + WIDGET_SPACING + FIELD_GAP;

        // Scale slider
        cy += LABEL_H;
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + SLIDER_H) {
            sliderDragging = true;
            sliderCardIndex = index;
            sliderContentWidth = cw;
            double ratio = Math.max(0, Math.min(1, (double) (mx - cx) / cw));
            double newVal = Math.round((0.5 + ratio * 1.5) / 0.1) * 0.1;
            entry.scale = Math.max(0.5, Math.min(2.0, newVal));
            config.requestSave();
            return;
        }
        cy += SLIDER_H + WIDGET_SPACING + FIELD_GAP;

        // Position preset
        cy += LABEL_H;
        int positionLabelW = textRenderer.getWidth("Position") + 8;
        int presetBtnX = cx + positionLabelW;
        int presetW = Math.min(180, cx + cw - presetBtnX);
        if (presetW >= 60) {
            if (mx >= presetBtnX && mx < presetBtnX + presetW && my >= cy && my < cy + PRESET_H) {
                entry.presetName = entry.getNextPreset();
                entry.applyPreset(entry.presetName);
                config.requestSave();
                playClickSound();
                return;
            }
        }
        cy += PRESET_H + WIDGET_SPACING + FIELD_GAP;

        // Edit Custom Position button
        boolean isCustom = entry.isCustomPosition();
        int editBtnX = presetBtnX;
        int editBtnW = presetW;
        if (isCustom && mx >= editBtnX && mx < editBtnX + editBtnW && my >= cy && my < cy + EDIT_BTN_H) {
            MinecraftClient.getInstance().setScreen(new HudPositionScreen(this, entry));
            playClickSound();
            return;
        }
        cy += EDIT_BTN_H + WIDGET_SPACING + FIELD_GAP;

        // Reset to Defaults button
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + RESET_BTN_H) {
            CounterEntry defaults = new CounterEntry("CPS Counter",
                "[LMB: %cps_mouse.left% | RMB: %cps_mouse.right%]",
                true, 0x80000000, 0xFFFFFFFF, 1.0, 0.0, 0.0, "Top-Left");
            entry.name = defaults.name;
            entry.displayFormat = defaults.displayFormat;
            entry.showBackground = defaults.showBackground;
            entry.backgroundColor = defaults.backgroundColor;
            entry.textColor = defaults.textColor;
            entry.scale = defaults.scale;
            entry.posX = defaults.posX;
            entry.posY = defaults.posY;
            entry.presetName = defaults.presetName;
            syncFieldTexts();
            config.requestSave();
            playClickSound();
            return;
        }
    }

    private void setCursorFromClick(int mx, int fieldX, int cardIndex, List<String> texts, List<Integer> cursors) {
        String text = texts.size() > cardIndex ? texts.get(cardIndex) : "";
        int innerX = mx - fieldX - 4;
        if (innerX <= 0) { cursors.set(cardIndex, 0); return; }
        int pos = 0;
        while (pos < text.length()) { int charW = textRenderer.getWidth(text.substring(0, pos + 1)); if (charW > innerX) break; pos++; }
        cursors.set(cardIndex, pos);
    }

    // ========== Text Field Helpers ==========

    private void drawTextField(DrawContext dc, String text, int cursor, int x, int y, int w, boolean focused) {
        dc.fill(x, y, x + w, y + 20, DrawUtils.FIELD_BG);
        DrawUtils.drawBorder(dc, x, y, w, 20, focused ? DrawUtils.ACCENT : DrawUtils.BORDER);
        int innerW = w - 8;
        int textBeforeCursorW = cursor > 0 ? textRenderer.getWidth(text.substring(0, cursor)) : 0;
        int scroll = 0;
        if (textRenderer.getWidth(text) > innerW) {
            if (textBeforeCursorW > scroll + innerW) { scroll = textBeforeCursorW - innerW; }
            if (textBeforeCursorW < scroll) { scroll = Math.max(0, textBeforeCursorW - innerW / 2); }
        }
        boolean showCursor = focused && (System.currentTimeMillis() - cursorBlinkStart) % 1000 < 530;
        dc.enableScissor(x + 4, y + 1, x + w - 4, y + 19);
        if (showCursor) {
            String beforeCursor = text.substring(0, Math.min(cursor, text.length()));
            String afterCursor = text.substring(Math.min(cursor, text.length()));
            dc.drawTextWithShadow(textRenderer, beforeCursor, x + 4 - scroll, y + 6, DrawUtils.TEXT_PRIMARY);
            int cursorX = x + 4 - scroll + textRenderer.getWidth(beforeCursor);
            dc.fill(cursorX, y + 5, cursorX + 1, y + 15, DrawUtils.CURSOR_COLOR);
            dc.drawTextWithShadow(textRenderer, afterCursor, cursorX, y + 6, DrawUtils.TEXT_PRIMARY);
        } else {
            dc.drawTextWithShadow(textRenderer, text, x + 4 - scroll, y + 6, DrawUtils.TEXT_PRIMARY);
        }
        dc.disableScissor();
    }

    private boolean editTextField(int key, List<String> texts, List<Integer> cursors, Consumer<String> setter) {
        if (focusedCardIndex < 0 || focusedCardIndex >= texts.size() || focusedCardIndex >= cursors.size()) return false;
        String text = texts.get(focusedCardIndex);
        int cursor = cursors.get(focusedCardIndex);
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursor > 0 && !text.isEmpty()) { text = text.substring(0, cursor - 1) + text.substring(cursor); cursor--; texts.set(focusedCardIndex, text); cursors.set(focusedCardIndex, cursor); setter.accept(text); ModConfig.getConfig().requestSave(); cursorBlinkStart = System.currentTimeMillis(); }
            return true;
        } else if (key == GLFW.GLFW_KEY_LEFT) { if (cursor > 0) cursors.set(focusedCardIndex, cursor - 1); cursorBlinkStart = System.currentTimeMillis(); return true;
        } else if (key == GLFW.GLFW_KEY_RIGHT) { if (cursor < text.length()) cursors.set(focusedCardIndex, cursor + 1); cursorBlinkStart = System.currentTimeMillis(); return true;
        } else if (key == GLFW.GLFW_KEY_DELETE) { if (cursor < text.length()) { text = text.substring(0, cursor) + text.substring(cursor + 1); texts.set(focusedCardIndex, text); setter.accept(text); ModConfig.getConfig().requestSave(); cursorBlinkStart = System.currentTimeMillis(); } return true;
        } else if (key == GLFW.GLFW_KEY_HOME) { cursors.set(focusedCardIndex, 0); cursorBlinkStart = System.currentTimeMillis(); return true;
        } else if (key == GLFW.GLFW_KEY_END) { cursors.set(focusedCardIndex, text.length()); cursorBlinkStart = System.currentTimeMillis(); return true;
        } else if (key == GLFW.GLFW_KEY_ENTER) { focusedCardIndex = -1; focusedFieldIndex = -1; return true; }
        return false;
    }

    private boolean charTypedField(char c, List<String> texts, List<Integer> cursors, Consumer<String> setter) {
        if (focusedCardIndex < 0 || focusedCardIndex >= texts.size() || focusedCardIndex >= cursors.size()) return false;
        if (c >= 32 && c != 127) {
            String text = texts.get(focusedCardIndex);
            int cursor = cursors.get(focusedCardIndex);
            text = text.substring(0, cursor) + c + text.substring(cursor);
            cursor++;
            texts.set(focusedCardIndex, text);
            cursors.set(focusedCardIndex, cursor);
            setter.accept(text);
            ModConfig.getConfig().requestSave();
            cursorBlinkStart = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    // ========== Input Handling ==========

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset += (int) (verticalAmount * 20);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (sliderDragging && sliderCardIndex >= 0 && sliderCardIndex < ModConfig.getConfig().counters.size()) {
            int cw = sliderContentWidth;
            int cx = SIDEBAR_WIDTH + PADDING + CARD_PADDING;
            double ratio = Math.max(0, Math.min(1, (double) ((int) click.x() - cx) / cw));
            double newVal = Math.round((0.5 + ratio * 1.5) / 0.1) * 0.1;
            ModConfig.getConfig().counters.get(sliderCardIndex).scale = Math.max(0.5, Math.min(2.0, newVal));
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (sliderDragging) { sliderDragging = false; sliderCardIndex = -1; ModConfig.getConfig().requestSave(); }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        if (focusedCardIndex >= 0 && focusedCardIndex < ModConfig.getConfig().counters.size()
         && focusedCardIndex < cardFieldTexts.size() && focusedCardIndex < nameFieldTexts.size()) {
            int key = keyInput.key();
            if (focusedFieldIndex == 0) { if (editTextField(key, cardFieldTexts, cardFieldCursors, t -> ModConfig.getConfig().counters.get(focusedCardIndex).displayFormat = t)) return true; }
            else if (focusedFieldIndex == 1) { if (editTextField(key, nameFieldTexts, nameFieldCursors, t -> ModConfig.getConfig().counters.get(focusedCardIndex).name = t)) return true; }
        } else {
            focusedCardIndex = -1;
            focusedFieldIndex = -1;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput charInput) {
        if (focusedCardIndex >= 0 && focusedCardIndex < ModConfig.getConfig().counters.size()
         && focusedCardIndex < cardFieldTexts.size() && focusedCardIndex < nameFieldTexts.size()) {
            char c = (char) charInput.codepoint();
            if (focusedFieldIndex == 0) { if (charTypedField(c, cardFieldTexts, cardFieldCursors, t -> ModConfig.getConfig().counters.get(focusedCardIndex).displayFormat = t)) return true; }
            else if (focusedFieldIndex == 1) { if (charTypedField(c, nameFieldTexts, nameFieldCursors, t -> ModConfig.getConfig().counters.get(focusedCardIndex).name = t)) return true; }
        } else {
            focusedCardIndex = -1;
            focusedFieldIndex = -1;
        }
        return super.charTyped(charInput);
    }

    @Override
    public void close() { ModConfig.getConfig().flushIfDirty(); if (client != null) client.setScreen(parent); }
    @Override
    public boolean shouldPause() { return false; }
}
