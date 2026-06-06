package com.notnawfas.ultimatepvputils.hud;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import com.notnawfas.ultimatepvputils.config.ShieldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ShieldCustomizeScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_PADDING = 12;
    private static final int SLIDER_TRACK_H = 8;
    private static final int SLIDER_KNOB_W = 10;
    private static final int SLIDER_KNOB_H = 14;
    private static final int ROW_HEIGHT = 36;
    private static final int LABEL_H = 14;
    private static final int SECTION_GAP = 16;
    private static final int PREVIEW_ITEM_SIZE = 16;

    private final Screen parent;
    private final ItemStack shieldStack = new ItemStack(Items.SHIELD);

    private boolean sliderDragging = false;
    private int activeSlider = -1;
    private int sliderTrackX = 0;
    private int sliderTrackW = 0;

    public ShieldCustomizeScreen(Screen parent) {
        super(Text.literal("Shield Customize"));
        this.parent = parent;
    }

    private ShieldConfig cfg() {
        return ModConfig.getConfig().shieldConfig;
    }

    @Override
    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        renderBackground(dc, mouseX, mouseY, delta);

        int panelX = width - PANEL_WIDTH;
        int panelY = 0;
        int panelH = height;

        dc.fill(panelX, panelY, panelX + PANEL_WIDTH, panelH, DrawUtils.OVERLAY_BG);
        dc.fill(panelX, panelY, panelX + 1, panelH, DrawUtils.BORDER);

        int cx = panelX + PANEL_PADDING;
        int cy = panelY + PANEL_PADDING;
        int cw = PANEL_WIDTH - PANEL_PADDING * 2;

        dc.drawTextWithShadow(textRenderer, "Shield Transform", cx, cy, DrawUtils.ACCENT);
        cy += SECTION_GAP;

        cy = drawSliderRow(dc, "Size", cfg().size, ShieldConfig.MIN_SIZE, ShieldConfig.MAX_SIZE, "%.1fx", 0, cx, cy, cw, mouseX, mouseY);
        cy = drawSliderRow(dc, "Offset X", cfg().offsetX, ShieldConfig.MIN_OFFSET, ShieldConfig.MAX_OFFSET, "%.0f", 1, cx, cy, cw, mouseX, mouseY);
        cy = drawSliderRow(dc, "Offset Y", cfg().offsetY, ShieldConfig.MIN_OFFSET, ShieldConfig.MAX_OFFSET, "%.0f", 2, cx, cy, cw, mouseX, mouseY);

        cy += 4;
        drawCheckboxRow(dc, "Visible", cfg().visible, cx, cy, cw, mouseX, mouseY);
        cy += ROW_HEIGHT;

        cy += 8;
        int btnW = cw;
        int btnH = 22;
        boolean resetHovered = mouseX >= cx && mouseX < cx + btnW && mouseY >= cy && mouseY < cy + btnH;
        dc.fill(cx, cy, cx + btnW, cy + btnH, resetHovered ? DrawUtils.RESET_HOVER : DrawUtils.RESET_BG);
        DrawUtils.drawBorder(dc, cx, cy, btnW, btnH, DrawUtils.RESET_BORDER);
        dc.drawCenteredTextWithShadow(textRenderer, "Reset to Defaults", cx + btnW / 2, cy + 7, DrawUtils.RESET_TEXT);

        drawShieldPreview(dc, panelX);
        dc.drawCenteredTextWithShadow(textRenderer, "ESC to confirm", panelX / 2, height - 20, DrawUtils.ACCENT_DIM);
    }

    private void drawShieldPreview(DrawContext dc, int panelX) {
        int previewW = panelX;
        int previewH = height;

        dc.drawCenteredTextWithShadow(textRenderer, "Shield Preview", previewW / 2, 12, DrawUtils.TEXT_DIM);

        int baseSize = PREVIEW_ITEM_SIZE * 4;
        float scale = (float) cfg().size;
        int itemW = (int) (baseSize * scale);
        int itemH = (int) (baseSize * scale);

        int centerX = previewW / 2 + (int) (cfg().offsetX * 2);
        int centerY = previewH / 2 - (int) (cfg().offsetY * 2);

        if (!cfg().visible) {
            dc.fill(centerX - itemW / 2 - 2, centerY - itemH / 2 - 2,
                    centerX + itemW / 2 + 2, centerY + itemH / 2 + 2, 0x40FF4444);
            dc.drawCenteredTextWithShadow(textRenderer, "Hidden", centerX, centerY - 4, 0xFFFF4444);
        } else {
            dc.getMatrices().pushMatrix();
            try {
                dc.getMatrices().translate(centerX - itemW / 2, centerY - itemH / 2);
                dc.getMatrices().scale(scale * 4, scale * 4);
                dc.drawItemWithoutEntity(shieldStack, 0, 0);
            } finally {
                dc.getMatrices().popMatrix();
            }
        }
    }

    private int drawSliderRow(DrawContext dc, String label, double value, double min, double max, String fmt, int id, int x, int y, int w, int mx, int my) {
        String displayVal = String.format(fmt, value);
        dc.drawTextWithShadow(textRenderer, label, x, y, DrawUtils.TEXT_LABEL);
        int valW = textRenderer.getWidth(displayVal);
        dc.drawTextWithShadow(textRenderer, displayVal, x + w - valW, y, DrawUtils.TEXT_PRIMARY);
        y += LABEL_H;

        drawSlider(dc, value, min, max, x, y, w, mx, my, id);

        y += SLIDER_KNOB_H + 8;
        return y;
    }

    private void drawSlider(DrawContext dc, double value, double min, double max, int x, int y, int w, int mx, int my, int id) {
        int trackY = y + (SLIDER_KNOB_H - SLIDER_TRACK_H) / 2;
        dc.fill(x, trackY, x + w, trackY + SLIDER_TRACK_H, DrawUtils.BORDER_LIGHT);

        double ratio = (max > min) ? (value - min) / (max - min) : 0;
        int filledW = (int) (w * ratio);
        dc.fill(x, trackY, x + filledW, trackY + SLIDER_TRACK_H, DrawUtils.ACCENT);

        int knobX = x + filledW - SLIDER_KNOB_W / 2;
        knobX = Math.max(x, Math.min(knobX, x + w - SLIDER_KNOB_W));

        boolean active = sliderDragging && activeSlider == id;
        boolean hovered = mx >= knobX && mx < knobX + SLIDER_KNOB_W && my >= y && my < y + SLIDER_KNOB_H;
        int knobColor = (active || hovered) ? DrawUtils.KNOB : DrawUtils.KNOB_ALT;
        dc.fill(knobX, y, knobX + SLIDER_KNOB_W, y + SLIDER_KNOB_H, knobColor);
        DrawUtils.drawBorder(dc, knobX, y, SLIDER_KNOB_W, SLIDER_KNOB_H, DrawUtils.BORDER_DIM);
    }

    private void drawCheckboxRow(DrawContext dc, String label, boolean value, int x, int y, int w, int mx, int my) {
        dc.drawTextWithShadow(textRenderer, label, x, y + 1, DrawUtils.TEXT_LABEL);
        int boxSize = 18;
        int boxX = x + w - boxSize;
        boolean hovered = mx >= boxX && mx < boxX + boxSize && my >= y && my < y + boxSize;
        dc.fill(boxX, y, boxX + boxSize, y + boxSize, hovered ? DrawUtils.BTN_HOVER : DrawUtils.BTN_BG);
        DrawUtils.drawBorder(dc, boxX, y, boxSize, boxSize, value ? DrawUtils.ACCENT : DrawUtils.BORDER);
        if (value) {
            dc.drawTextWithShadow(textRenderer, "\u2713", boxX + 5, y + 4, DrawUtils.ACCENT);
        }
    }

    @Override
    public void renderBackground(DrawContext dc, int mouseX, int mouseY, float delta) {
        if (this.client != null && this.client.world != null) {
            this.client.gameRenderer.renderWorld(this.client.getRenderTickCounter());
        } else {
            dc.fill(0, 0, width, height, DrawUtils.CARD_BG);
        }
    }

    private int[] getSliderHitArea(int id) {
        int panelX = width - PANEL_WIDTH;
        int cx = panelX + PANEL_PADDING;
        int cw = PANEL_WIDTH - PANEL_PADDING * 2;
        int cy = PANEL_PADDING + SECTION_GAP;

        for (int i = 0; i < id; i++) {
            cy += ROW_HEIGHT;
        }
        int sliderY = cy + LABEL_H;
        return new int[]{cx, sliderY, cw, SLIDER_KNOB_H};
    }

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, always);
        int mx = (int) click.x(), my = (int) click.y();

        for (int id = 0; id < 3; id++) {
            int[] area = getSliderHitArea(id);
            if (mx >= area[0] && mx < area[0] + area[2] && my >= area[1] - 2 && my < area[1] + area[3] + 2) {
                sliderDragging = true;
                activeSlider = id;
                sliderTrackX = area[0];
                sliderTrackW = area[2];
                double ratio = Math.max(0, Math.min(1, (double) (mx - sliderTrackX) / sliderTrackW));
                setSliderValue(id, ratio);
                ModConfig.getConfig().requestSave();
                return true;
            }
        }

        int panelX = width - PANEL_WIDTH;
        int cx = panelX + PANEL_PADDING;
        int cw = PANEL_WIDTH - PANEL_PADDING * 2;
        int checkboxY = PANEL_PADDING + SECTION_GAP + ROW_HEIGHT * 3 + 4;
        int boxSize = 18;
        int boxX = cx + cw - boxSize;
        if (mx >= boxX && mx < boxX + boxSize && my >= checkboxY && my < checkboxY + boxSize) {
            cfg().visible = !cfg().visible;
            ModConfig.getConfig().requestSave();
            return true;
        }

        int cy = checkboxY + ROW_HEIGHT + 8;
        int btnW = cw;
        int btnH = 22;
        if (mx >= cx && mx < cx + btnW && my >= cy && my < cy + btnH) {
            cfg().resetToDefaults();
            ModConfig.getConfig().requestSave();
            return true;
        }

        return super.mouseClicked(click, always);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (sliderDragging && activeSlider >= 0) {
            int mx = (int) click.x();
            double ratio = Math.max(0, Math.min(1, (double) (mx - sliderTrackX) / sliderTrackW));
            setSliderValue(activeSlider, ratio);
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (sliderDragging) {
            sliderDragging = false;
            activeSlider = -1;
            ModConfig.getConfig().requestSave();
            return true;
        }
        return super.mouseReleased(click);
    }

    private double getSliderValue(int id) {
        return switch (id) {
            case 0 -> cfg().size;
            case 1 -> cfg().offsetX;
            case 2 -> cfg().offsetY;
            default -> 0;
        };
    }

    private void setSliderValue(int id, double ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        switch (id) {
            case 0 -> {
                double range = ShieldConfig.MAX_SIZE - ShieldConfig.MIN_SIZE;
                cfg().size = Math.round((ShieldConfig.MIN_SIZE + ratio * range) * 10.0) / 10.0;
            }
            case 1 -> {
                double range = ShieldConfig.MAX_OFFSET - ShieldConfig.MIN_OFFSET;
                cfg().offsetX = Math.round(ShieldConfig.MIN_OFFSET + ratio * range);
            }
            case 2 -> {
                double range = ShieldConfig.MAX_OFFSET - ShieldConfig.MIN_OFFSET;
                cfg().offsetY = Math.round(ShieldConfig.MIN_OFFSET + ratio * range);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return true;
    }

    @Override
    public void close() {
        ModConfig.getConfig().save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
