package com.notnawfas.ultimatepvputils.config;

import com.notnawfas.ultimatepvputils.hud.ColorPickerScreen;
import com.notnawfas.ultimatepvputils.hud.HudPositionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModConfigScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 120;
    private static final int PADDING = 12;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_SPACING = 6;
    private static final int CARD_PADDING = 8;
    private static final String[] PRESETS = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Custom"};

    private final Screen parent;
    private int expandedCardIndex = -1;

    private int focusedCardIndex = -1;
    private int focusedFieldIndex = -1;
    private final List<String> cardFieldTexts = new ArrayList<>();
    private final List<Integer> cardFieldCursors = new ArrayList<>();
    private final List<String> nameFieldTexts = new ArrayList<>();
    private final List<Integer> nameFieldCursors = new ArrayList<>();

    private boolean sliderDragging = false;
    private int sliderCardIndex = -1;

    private int scrollOffset = 0;

    private int pendingDeleteIndex = -1;
    private long pendingDeleteTime = 0;

    public ModConfigScreen(Screen parent) {
        super(Text.literal("Ultimate PvP Utils"));
        this.parent = parent;
        syncFieldTexts();
    }

    private void syncFieldTexts() {
        List<CounterEntry> counters = ModConfig.getConfig().counters;
        while (cardFieldTexts.size() < counters.size()) { cardFieldTexts.add(""); cardFieldCursors.add(0); nameFieldTexts.add(""); nameFieldCursors.add(0); }
        while (cardFieldTexts.size() > counters.size()) { cardFieldTexts.remove(cardFieldTexts.size() - 1); cardFieldCursors.remove(cardFieldCursors.size() - 1); nameFieldTexts.remove(nameFieldTexts.size() - 1); nameFieldCursors.remove(nameFieldCursors.size() - 1); }
        for (int i = 0; i < counters.size(); i++) {
            if (focusedCardIndex != i || focusedFieldIndex != 0) cardFieldTexts.set(i, counters.get(i).displayFormat);
            if (cardFieldCursors.get(i) > cardFieldTexts.get(i).length()) cardFieldCursors.set(i, cardFieldTexts.get(i).length());
            if (focusedCardIndex != i || focusedFieldIndex != 1) nameFieldTexts.set(i, counters.get(i).name);
            if (nameFieldCursors.get(i) > nameFieldTexts.get(i).length()) nameFieldCursors.set(i, nameFieldTexts.get(i).length());
        }
    }

    private int getTotalContentHeight() {
        List<CounterEntry> counters = ModConfig.getConfig().counters;
        if (counters.isEmpty()) return 32 + 40 + 22 + WIDGET_SPACING;
        int h = 32;
        for (int i = 0; i < counters.size(); i++) {
            h += (i == expandedCardIndex ? getExpandedCardHeight() : 28) + WIDGET_SPACING;
        }
        h += 4 + 22;
        return h;
    }

    @Override
    public void render(DrawContext dc, int mx, int my, float delta) {
        super.render(dc, mx, my, delta);
        drawSidebar(dc, mx, my);
        drawCpsTab(dc, ModConfig.getConfig(), SIDEBAR_WIDTH + PADDING, width - SIDEBAR_WIDTH - PADDING * 2, mx, my);
        dc.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext dc, int mx, int my, float delta) {
        dc.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawSidebar(DrawContext dc, int mx, int my) {
        int x = 0, y = 24;
        dc.fill(x, y, x + SIDEBAR_WIDTH, height, 0xC0101010);

        ModConfig config = ModConfig.getConfig();
        drawToggleRow(dc, "HUD Enabled", config.enabled, x + 8, y + 4, SIDEBAR_WIDTH - 16, mx, my);
        y += WIDGET_HEIGHT + 12;

        dc.fill(x, y, x + SIDEBAR_WIDTH, y + 1, 0x30FFFFFF);
        y += 8;

        boolean hovered = mx >= x && mx < x + SIDEBAR_WIDTH && my >= y && my < y + 28;
        if (hovered) dc.fill(x, y, x + SIDEBAR_WIDTH, y + 28, 0x20FFFFFF);
        dc.fill(x, y, x + 2, y + 28, 0xFF4FC3F7);
        dc.drawTextWithShadow(textRenderer, "CPS Counter", x + 14, y + 9, 0xFF4FC3F7);
    }

    private int getExpandedCardHeight() {
        return 28 + CARD_PADDING
                + 14 + 20 + 4
                + 14 + 20 + 4
                + 18
                + WIDGET_HEIGHT + WIDGET_SPACING
                + 14 + 20 + WIDGET_SPACING + 4
                + 14 + 20
                + WIDGET_SPACING + 8
                + 14 + 20
                + WIDGET_SPACING + 8
                + 14 + 22 + WIDGET_SPACING
                + (22 + WIDGET_SPACING)
                + CARD_PADDING;
    }

    private void drawCpsTab(DrawContext dc, ModConfig config, int x, int w, int mx, int my) {
        int maxScroll = Math.max(0, getTotalContentHeight() - (height - 24));
        scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));

        int cy = 32 + scrollOffset;
        List<CounterEntry> counters = config.counters;

        if (counters.isEmpty()) {
            dc.drawCenteredTextWithShadow(textRenderer, "No Counters Found", x + w / 2, cy + 20, 0xFF888888);
            cy += 40;
            drawAddButton(dc, x, cy, w, mx, my);
            return;
        }

        for (int i = 0; i < counters.size(); i++) {
            CounterEntry entry = counters.get(i);
            boolean expanded = (i == expandedCardIndex);
            int cardH = expanded ? getExpandedCardHeight() : 28;
            drawCard(dc, entry, i, expanded, x, cy, w, cardH, mx, my);
            cy += cardH + WIDGET_SPACING;
        }

        drawAddButton(dc, x, cy + 4, w, mx, my);
    }

    private void drawCard(DrawContext dc, CounterEntry entry, int index, boolean expanded, int x, int y, int w, int cardH, int mx, int my) {
        dc.fill(x, y, x + w, y + cardH, 0xFF1E1E1E);
        dc.fill(x, y, x + w, y + 1, 0xFF444444);
        dc.fill(x, y + cardH - 1, x + w, y + cardH, 0xFF333333);
        dc.fill(x, y, x + 1, y + cardH, 0xFF444444);
        dc.fill(x + w - 1, y, x + w, y + cardH, 0xFF444444);

        String arrow = expanded ? "\u25BC" : "\u25B6";
        boolean arrowHovered = mx >= x && mx < x + 24 && my >= y && my < y + 28;
        dc.drawTextWithShadow(textRenderer, arrow, x + 6, y + 9, arrowHovered ? 0xFF4FC3F7 : 0xFFAAAAAA);
        dc.drawTextWithShadow(textRenderer, entry.name, x + 24, y + 9, 0xFFE0E0E0);

        int deleteX = x + w - 18;
        boolean deleteHovered = mx >= deleteX && mx < x + w && my >= y && my < y + 28;

        if (pendingDeleteIndex == index && System.currentTimeMillis() - pendingDeleteTime < 3000) {
            dc.drawTextWithShadow(textRenderer, "\u2715", deleteX, y + 9, 0xFFFF8800);
            int confirmX = deleteX - 50;
            dc.drawTextWithShadow(textRenderer, "Confirm?", confirmX, y + 9, 0xFFFF8800);
        } else {
            dc.drawTextWithShadow(textRenderer, "\u2715", deleteX, y + 9, deleteHovered ? 0xFFFF4444 : 0xFF666666);
        }

        if (!expanded) return;

        int cx = x + CARD_PADDING;
        int cw = w - CARD_PADDING * 2;
        int cy = y + 28 + CARD_PADDING;

        // Name
        dc.drawTextWithShadow(textRenderer, "Name:", cx, cy, 0xFFCCCCCC);
        cy += 14;
        String nameText = nameFieldTexts.size() > index ? nameFieldTexts.get(index) : entry.name;
        dc.fill(cx, cy, cx + cw, cy + 20, 0xFF1A1A1A);
        drawBorder(dc, cx, cy, cw, 20, focusedCardIndex == index && focusedFieldIndex == 1 ? 0xFF4FC3F7 : 0xFF444444);
        String nameDisplay = nameText;
        if (focusedCardIndex == index && focusedFieldIndex == 1 && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursor = nameFieldCursors.size() > index ? nameFieldCursors.get(index) : nameText.length();
            nameDisplay = nameText.substring(0, Math.min(cursor, nameText.length())) + "|" + nameText.substring(Math.min(cursor, nameText.length()));
        }
        dc.drawTextWithShadow(textRenderer, nameDisplay, cx + 4, cy + 6, 0xFFE0E0E0);
        cy += 20 + 4;

        // Display Format
        dc.drawTextWithShadow(textRenderer, "Display Format:", cx, cy, 0xFFCCCCCC);
        cy += 14;
        String formatText = cardFieldTexts.size() > index ? cardFieldTexts.get(index) : entry.displayFormat;
        dc.fill(cx, cy, cx + cw, cy + 20, 0xFF1A1A1A);
        drawBorder(dc, cx, cy, cw, 20, focusedCardIndex == index && focusedFieldIndex == 0 ? 0xFF4FC3F7 : 0xFF444444);
        String fmtDisplay = formatText;
        if (focusedCardIndex == index && focusedFieldIndex == 0 && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursor = cardFieldCursors.size() > index ? cardFieldCursors.get(index) : formatText.length();
            fmtDisplay = formatText.substring(0, Math.min(cursor, formatText.length())) + "|" + formatText.substring(Math.min(cursor, formatText.length()));
        }
        dc.drawTextWithShadow(textRenderer, fmtDisplay, cx + 4, cy + 6, 0xFFE0E0E0);
        cy += 20 + 4;

        dc.drawTextWithShadow(textRenderer, "Available Variables can be found in https://notnawfas.qzz.io/blog/ultimatepvputils", cx, cy, 0xFF888888);
        cy += 18;

        drawToggleRow(dc, "Show Background", entry.showBackground, cx, cy, cw, mx, my);
        cy += WIDGET_HEIGHT + WIDGET_SPACING;

        int halfW = (cw - 8) / 2;
        dc.drawTextWithShadow(textRenderer, "Background Color", cx, cy, 0xFFCCCCCC);
        cy += 14;
        drawColorSwatch(dc, entry.backgroundColor, cx, cy, halfW, mx, my);
        dc.drawTextWithShadow(textRenderer, "Text Color", cx + halfW + 8, cy - 14, 0xFFCCCCCC);
        drawColorSwatch(dc, entry.textColor, cx + halfW + 8, cy, halfW, mx, my);
        cy += WIDGET_HEIGHT + WIDGET_SPACING + 4;

        dc.drawTextWithShadow(textRenderer, "Scale: " + String.format("%.1fx", entry.scale), cx, cy, 0xFFCCCCCC);
        cy += 14;
        drawSlider(dc, entry.scale, cx, cy, cw, mx, my);
        cy += WIDGET_SPACING + 8;

        // Position
        dc.drawTextWithShadow(textRenderer, "Position", cx, cy, 0xFFCCCCCC);
        cy += 14;
        int presetW = 180, presetH = 22;
        boolean presetHovered = mx >= cx && mx < cx + presetW && my >= cy && my < cy + presetH;
        dc.fill(cx, cy, cx + presetW, cy + presetH, presetHovered ? 0xFF3A3A3A : 0xFF2A2A2A);
        drawBorder(dc, cx, cy, presetW, presetH, 0xFF555555);
        dc.drawCenteredTextWithShadow(textRenderer, "Preset: " + entry.presetName, cx + presetW / 2, cy + 7, 0xFF4FC3F7);
        cy += presetH + WIDGET_SPACING;

        if (entry.isCustomPosition()) {
            int editBtnH = 22;
            boolean editHovered = mx >= cx && mx < cx + cw && my >= cy && my < cy + editBtnH;
            dc.fill(cx, cy, cx + cw, cy + editBtnH, editHovered ? 0xFF3A3A3A : 0xFF2A2A2A);
            drawBorder(dc, cx, cy, cw, editBtnH, 0xFF555555);
            dc.drawCenteredTextWithShadow(textRenderer, "Edit Custom Position", cx + cw / 2, cy + 7, 0xFFE0E0E0);
        }
    }

    private void drawToggleRow(DrawContext dc, String label, boolean value, int x, int y, int w, int mx, int my) {
        int toggleW = 40, toggleH = 20, toggleX = x + w - toggleW;
        boolean hovered = mx >= toggleX && mx < toggleX + toggleW && my >= y && my < y + toggleH;
        dc.drawTextWithShadow(textRenderer, label, x, y + 6, 0xFFCCCCCC);
        dc.fill(toggleX, y + 2, toggleX + toggleW, y + toggleH - 2, value ? 0xFF4FC3F7 : 0xFF555555);
        dc.fill(value ? toggleX + toggleW - 12 : toggleX + 2, y + 3, value ? toggleX + toggleW - 2 : toggleX + 12, y + toggleH - 3, 0xFFFFFFFF);
        if (hovered) dc.fill(toggleX, y, toggleX + toggleW, y + toggleH, 0x10FFFFFF);
    }

    private void drawColorSwatch(DrawContext dc, int color, int x, int y, int w, int mx, int my) {
        int h = 20;
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        dc.fill(x, y, x + w, y + h, color | 0xFF000000);
        drawBorder(dc, x, y, w, h, 0xFF000000);
        if (hovered) dc.fill(x, y, x + w, y + h, 0x20FFFFFF);
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        String hex = String.format("#%02X%02X%02X", r, g, b);
        dc.drawCenteredTextWithShadow(textRenderer, hex, x + w / 2, y + 6, (r * 0.299 + g * 0.587 + b * 0.114) > 128 ? 0xFF000000 : 0xFFFFFFFF);
    }

    private void drawSlider(DrawContext dc, double value, int x, int y, int w, int mx, int my) {
        dc.fill(x, y + 6, x + w, y + 14, 0xFF333333);
        double ratio = (value - 0.5) / 1.5;
        int filledW = (int) (w * ratio);
        dc.fill(x, y + 6, x + filledW, y + 14, 0xFF4FC3F7);
        dc.fill(x + filledW - 4, y + 3, x + filledW + 4, y + 17, 0xFFFFFFFF);
    }

    private void drawAddButton(DrawContext dc, int x, int y, int w, int mx, int my) {
        int btnH = 22;
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + btnH;
        dc.fill(x, y, x + w, y + btnH, hovered ? 0xFF2A4A2A : 0xFF1E3A1E);
        drawBorder(dc, x, y, w, btnH, 0xFF4CAF50);
        dc.drawCenteredTextWithShadow(textRenderer, "(+) Add New Counter", x + w / 2, y + 7, 0xFF4CAF50);
    }

    private void drawBorder(DrawContext dc, int x, int y, int w, int h, int color) {
        dc.fill(x, y, x + w, y + 1, color);
        dc.fill(x, y + h - 1, x + w, y + h, color);
        dc.fill(x, y, x + 1, y + h, color);
        dc.fill(x + w - 1, y, x + w, y + h, color);
    }

    // ========== Click Handling ==========

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != 0) return super.mouseClicked(click, always);
        int mx = (int) click.x(), my = (int) click.y();

        // Sidebar toggle
        ModConfig config = ModConfig.getConfig();
        int toggleX = 8, toggleY = 28, toggleW = SIDEBAR_WIDTH - 16, toggleH = 20;
        if (mx >= toggleX && mx < toggleX + toggleW && my >= toggleY && my < toggleY + toggleH) {
            config.enabled = !config.enabled; config.save(); return true;
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
                counters.add(new CounterEntry("Counter " + (counters.size() + 1), "%cps_mouse.left%", true, 0x80000000, 0xFFFFFFFF, 1.0, 0.0, 0.0, "Top-Left"));
                syncFieldTexts(); config.save();
            }
            return;
        }

        for (int i = 0; i < counters.size(); i++) {
            boolean expanded = (i == expandedCardIndex);
            int cardH = expanded ? getExpandedCardHeight() : 28;
            if (my >= cy && my < cy + cardH && mx >= x && mx < x + w) {
                if (my < cy + 28) {
                    // Delete
                    if (mx >= x + w - 18) {
                        if (pendingDeleteIndex == i && System.currentTimeMillis() - pendingDeleteTime < 3000) {
                            counters.remove(i);
                            if (expandedCardIndex == i) expandedCardIndex = -1;
                            else if (expandedCardIndex > i) expandedCardIndex--;
                            pendingDeleteIndex = -1;
                            syncFieldTexts(); config.save();
                        } else {
                            pendingDeleteIndex = i;
                            pendingDeleteTime = System.currentTimeMillis();
                        }
                        return;
                    }
                    // Reset pending delete if clicking elsewhere
                    pendingDeleteIndex = -1;
                    expandedCardIndex = (expandedCardIndex == i) ? -1 : i;
                    if (expandedCardIndex == i) syncFieldTexts();
                    return;
                }
                if (expanded) handleExpandedCardClick(mx, my, x, cy + 28, w, i, counters.get(i), config);
                return;
            }
            cy += cardH + WIDGET_SPACING;
        }

        pendingDeleteIndex = -1;
        int addBtnY = cy + 4;
        if (mx >= x && mx < x + w && my >= addBtnY && my < addBtnY + 22) {
            counters.add(new CounterEntry("Counter " + (counters.size() + 1), "%cps_mouse.left%", true, 0x80000000, 0xFFFFFFFF, 1.0, 0.0, 0.0, "Top-Left"));
            syncFieldTexts(); config.save();
        }
    }

    private void handleExpandedCardClick(int mx, int my, int cardX, int cardBodyY, int cardW, int index, CounterEntry entry, ModConfig config) {
        int cx = cardX + CARD_PADDING;
        int cw = cardW - CARD_PADDING * 2;
        int cy = cardBodyY + CARD_PADDING;

        // Name label
        cy += 14;
        // Name field
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + 20) { focusedCardIndex = index; focusedFieldIndex = 1; return; }
        cy += 20 + 4;

        // Display Format label
        cy += 14;
        // Display Format field
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + 20) { focusedCardIndex = index; focusedFieldIndex = 0; return; }
        cy += 20 + 4;

        // Variables hint
        cy += 18;

        // Show Background toggle
        int toggleW = 40, toggleX = cx + cw - toggleW;
        if (mx >= toggleX && mx < toggleX + toggleW && my >= cy && my < cy + WIDGET_HEIGHT) {
            entry.showBackground = !entry.showBackground; config.save(); return;
        }
        cy += WIDGET_HEIGHT + WIDGET_SPACING;

        // Background Color label
        cy += 14;
        int halfW = (cw - 8) / 2;
        // Background Color swatch
        if (mx >= cx && mx < cx + halfW && my >= cy && my < cy + 20) {
            final int idx = index;
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, "Background Color", entry.backgroundColor, c -> { ModConfig.getConfig().counters.get(idx).backgroundColor = c; ModConfig.getConfig().save(); }));
            return;
        }
        // Text Color swatch
        if (mx >= cx + halfW + 8 && mx < cx + cw && my >= cy && my < cy + 20) {
            final int idx = index;
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(this, "Text Color", entry.textColor, c -> { ModConfig.getConfig().counters.get(idx).textColor = c; ModConfig.getConfig().save(); }));
            return;
        }
        cy += WIDGET_HEIGHT + WIDGET_SPACING + 4;

        // Scale label
        cy += 14;
        // Scale slider
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + 20) {
            sliderDragging = true; sliderCardIndex = index;
            double ratio = (double) (mx - cx) / cw;
            double newVal = Math.round((0.5 + ratio * 1.5) / 0.1) * 0.1;
            entry.scale = Math.max(0.5, Math.min(2.0, newVal)); config.save(); return;
        }
        cy += WIDGET_SPACING + 8;

        // Position label
        cy += 14;
        // Position preset button
        int presetW = 180, presetH = 22;
        if (mx >= cx && mx < cx + presetW && my >= cy && my < cy + presetH) {
            entry.presetName = getNextPreset(entry.presetName);
            entry.applyPreset(entry.presetName);
            config.save(); return;
        }
        cy += presetH + WIDGET_SPACING;

        // Edit Custom Position button
        if (entry.isCustomPosition()) {
            int editBtnH = 22;
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + editBtnH) {
                MinecraftClient.getInstance().setScreen(new HudPositionScreen(this, entry));
                return;
            }
        }
    }

    private String getNextPreset(String current) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i].equals(current)) return PRESETS[(i + 1) % PRESETS.length];
        }
        return "Top-Left";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset += (int) (verticalAmount * 20);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (sliderDragging && sliderCardIndex >= 0 && sliderCardIndex < ModConfig.getConfig().counters.size()) {
            int cx = SIDEBAR_WIDTH + PADDING + CARD_PADDING;
            int cw = width - SIDEBAR_WIDTH - PADDING * 2 - CARD_PADDING * 2;
            double ratio = Math.max(0, Math.min(1, (double) ((int) click.x() - cx) / cw));
            double newVal = Math.round((0.5 + ratio * 1.5) / 0.1) * 0.1;
            ModConfig.getConfig().counters.get(sliderCardIndex).scale = Math.max(0.5, Math.min(2.0, newVal));
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (sliderDragging) {
            sliderDragging = false; sliderCardIndex = -1;
            ModConfig.getConfig().save();
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        if (focusedCardIndex >= 0 && focusedCardIndex < ModConfig.getConfig().counters.size()) {
            int key = keyInput.key();
            if (focusedFieldIndex == 0) {
                String text = cardFieldTexts.get(focusedCardIndex);
                int cursor = cardFieldCursors.get(focusedCardIndex);
                if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    if (cursor > 0 && !text.isEmpty()) { text = text.substring(0, cursor - 1) + text.substring(cursor); cursor--; cardFieldTexts.set(focusedCardIndex, text); cardFieldCursors.set(focusedCardIndex, cursor); ModConfig.getConfig().counters.get(focusedCardIndex).displayFormat = text; ModConfig.getConfig().save(); }
                    return true;
                } else if (key == GLFW.GLFW_KEY_LEFT) { if (cursor > 0) cardFieldCursors.set(focusedCardIndex, cursor - 1); return true;
                } else if (key == GLFW.GLFW_KEY_RIGHT) { if (cursor < text.length()) cardFieldCursors.set(focusedCardIndex, cursor + 1); return true;
                } else if (key == GLFW.GLFW_KEY_DELETE) { if (cursor < text.length()) { text = text.substring(0, cursor) + text.substring(cursor + 1); cardFieldTexts.set(focusedCardIndex, text); ModConfig.getConfig().counters.get(focusedCardIndex).displayFormat = text; ModConfig.getConfig().save(); } return true;
                } else if (key == GLFW.GLFW_KEY_ENTER) { focusedCardIndex = -1; focusedFieldIndex = -1; return true; }
            } else if (focusedFieldIndex == 1) {
                String text = nameFieldTexts.get(focusedCardIndex);
                int cursor = nameFieldCursors.get(focusedCardIndex);
                if (key == GLFW.GLFW_KEY_BACKSPACE) {
                    if (cursor > 0 && !text.isEmpty()) { text = text.substring(0, cursor - 1) + text.substring(cursor); cursor--; nameFieldTexts.set(focusedCardIndex, text); nameFieldCursors.set(focusedCardIndex, cursor); ModConfig.getConfig().counters.get(focusedCardIndex).name = text; ModConfig.getConfig().save(); }
                    return true;
                } else if (key == GLFW.GLFW_KEY_LEFT) { if (cursor > 0) nameFieldCursors.set(focusedCardIndex, cursor - 1); return true;
                } else if (key == GLFW.GLFW_KEY_RIGHT) { if (cursor < text.length()) nameFieldCursors.set(focusedCardIndex, cursor + 1); return true;
                } else if (key == GLFW.GLFW_KEY_DELETE) { if (cursor < text.length()) { text = text.substring(0, cursor) + text.substring(cursor + 1); nameFieldTexts.set(focusedCardIndex, text); ModConfig.getConfig().counters.get(focusedCardIndex).name = text; ModConfig.getConfig().save(); } return true;
                } else if (key == GLFW.GLFW_KEY_ENTER) { focusedCardIndex = -1; focusedFieldIndex = -1; return true; }
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput charInput) {
        if (focusedCardIndex >= 0 && focusedCardIndex < ModConfig.getConfig().counters.size()) {
            char c = (char) charInput.codepoint();
            if (c >= 32 && c != 127) {
                if (focusedFieldIndex == 0) {
                    String text = cardFieldTexts.get(focusedCardIndex); int cursor = cardFieldCursors.get(focusedCardIndex);
                    text = text.substring(0, cursor) + c + text.substring(cursor); cursor++;
                    cardFieldTexts.set(focusedCardIndex, text); cardFieldCursors.set(focusedCardIndex, cursor);
                    ModConfig.getConfig().counters.get(focusedCardIndex).displayFormat = text; ModConfig.getConfig().save(); return true;
                } else if (focusedFieldIndex == 1) {
                    String text = nameFieldTexts.get(focusedCardIndex); int cursor = nameFieldCursors.get(focusedCardIndex);
                    text = text.substring(0, cursor) + c + text.substring(cursor); cursor++;
                    nameFieldTexts.set(focusedCardIndex, text); nameFieldCursors.set(focusedCardIndex, cursor);
                    ModConfig.getConfig().counters.get(focusedCardIndex).name = text; ModConfig.getConfig().save(); return true;
                }
            }
        }
        return super.charTyped(charInput);
    }

    @Override
    public void close() { ModConfig.getConfig().save(); if (client != null) client.setScreen(parent); }
    @Override
    public boolean shouldPause() { return false; }
}
