package com.notnawfas.ultimatepvputils.config;

import com.notnawfas.ultimatepvputils.hud.CpsHudOverlay;
import com.notnawfas.ultimatepvputils.hud.HudPositionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.Color;

public class ModConfigScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 120;
    private static final int PADDING = 12;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_SPACING = 6;

    private final Screen parent;
    private int selectedTab = 0;

    private boolean formatFieldFocused = false;
    private String formatFieldText;
    private int formatFieldCursor = 0;
    private long formatFieldLastClick = 0;

    public ModConfigScreen(Screen parent) {
        super(Text.literal("Ultimate PvP Utils"));
        this.parent = parent;
        this.formatFieldText = ModConfig.getConfig().displayFormat;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        drawSidebar(drawContext, mouseX, mouseY);
        drawContent(drawContext, mouseX, mouseY, delta);

        drawContext.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawSidebar(DrawContext drawContext, int mouseX, int mouseY) {
        int x = 0;
        int y = 24;

        drawContext.fill(x, y, x + SIDEBAR_WIDTH, height, 0xC0101010);

        String[] tabs = {"CPS Counter", "Position"};
        for (int i = 0; i < tabs.length; i++) {
            int tabY = y + i * 28;
            boolean hovered = mouseX >= x && mouseX < x + SIDEBAR_WIDTH && mouseY >= tabY && mouseY < tabY + 28;
            boolean selected = (i == selectedTab);

            if (selected) {
                drawContext.fill(x, tabY, x + SIDEBAR_WIDTH, tabY + 28, 0x40FFFFFF);
                drawContext.fill(x, tabY, x + 2, tabY + 28, 0xFF4FC3F7);
            } else if (hovered) {
                drawContext.fill(x, tabY, x + SIDEBAR_WIDTH, tabY + 28, 0x20FFFFFF);
            }

            drawContext.drawTextWithShadow(textRenderer, tabs[i], x + 14, tabY + 9,
                    selected ? 0xFF4FC3F7 : 0xFFAAAAAA);
        }
    }

    private void drawContent(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        ModConfig config = ModConfig.getConfig();
        int contentX = SIDEBAR_WIDTH + PADDING;
        int contentY = 32;
        int contentWidth = width - SIDEBAR_WIDTH - PADDING * 2;

        if (selectedTab == 0) {
            drawCpsTab(drawContext, config, contentX, contentY, contentWidth, mouseX, mouseY);
        } else {
            drawPositionTab(drawContext, config, contentX, contentY, contentWidth, mouseX, mouseY);
        }
    }

    private void drawCpsTab(DrawContext drawContext, ModConfig config, int x, int y, int w, int mouseX, int mouseY) {
        int cy = y;

        cy = drawToggle(drawContext, "Enabled", config.enabled, x, cy, w, mouseX, mouseY, () -> {
            config.enabled = !config.enabled;
            config.save();
        });

        cy += WIDGET_SPACING + 4;

        drawContext.drawTextWithShadow(textRenderer, "Display Format", x, cy, 0xFFCCCCCC);
        cy += 14;
        cy = drawTextField(drawContext, formatFieldText, x, cy, w, mouseX, mouseY);
        cy += 4;
        drawContext.drawTextWithShadow(textRenderer, "Variables: %cps_mouse.left%  %cps_mouse.right%  %cps_keyboard.<key>%", x, cy, 0xFF888888);
        cy += 18;

        cy = drawToggle(drawContext, "Show Background", config.showBackground, x, cy, w, mouseX, mouseY, () -> {
            config.showBackground = !config.showBackground;
            config.save();
        });

        cy = drawToggle(drawContext, "Use Minecraft Font", config.useMinecraftFont, x, cy, w, mouseX, mouseY, () -> {
            config.useMinecraftFont = !config.useMinecraftFont;
            config.save();
        });

        cy += WIDGET_SPACING + 4;

        drawContext.drawTextWithShadow(textRenderer, "Background Color", x, cy, 0xFFCCCCCC);
        cy += 14;
        cy = drawColorButton(drawContext, config.backgroundColor, x, cy, w, mouseX, mouseY, newColor -> {
            config.backgroundColor = newColor;
            config.save();
        });

        drawContext.drawTextWithShadow(textRenderer, "Text Color", x + w / 2 + 8, cy - 14, 0xFFCCCCCC);
        cy = drawColorButton(drawContext, config.textColor, x + w / 2 + 8, cy - 14, w / 2 - 8, mouseX, mouseY, newColor -> {
            config.textColor = newColor;
            config.save();
        });

        cy += WIDGET_SPACING + 8;

        drawContext.drawTextWithShadow(textRenderer, "Scale: " + String.format("%.1fx", config.scale), x, cy, 0xFFCCCCCC);
        cy += 14;
        cy = drawSlider(drawContext, config.scale, 0.5, 2.0, 0.1, x, cy, w, mouseX, mouseY, newVal -> {
            config.scale = newVal;
            config.save();
        });

        cy += WIDGET_SPACING + 8;

        cy = drawButton(drawContext, "Edit Position...", x, cy, w, mouseX, mouseY, () -> {
            MinecraftClient.getInstance().setScreen(new HudPositionScreen(this));
        });
    }

    private void drawPositionTab(DrawContext drawContext, ModConfig config, int x, int y, int w, int mouseX, int mouseY) {
        drawContext.drawTextWithShadow(textRenderer, "Preset Positions", x, y, 0xFFCCCCCC);
        y += 18;

        String[][] labels = {
                {"Top-Left", "Top-Center", "Top-Right"},
                {"Mid-Left", "Center", "Mid-Right"},
                {"Bottom-Left", "Bottom-Center", "Bottom-Right"}
        };
        double[][] positions = {
                {0.0, 0.5, 1.0},
                {0.0, 0.5, 1.0},
                {0.0, 0.5, 1.0}
        };
        double[][] posY = {
                {0.0, 0.0, 0.0},
                {0.5, 0.5, 0.5},
                {1.0, 1.0, 1.0}
        };

        int cellW = (w - 8) / 3;
        int cellH = 44;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = x + col * (cellW + 4);
                int cy = y + row * (cellH + 4);
                boolean hovered = mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH;

                drawContext.fill(cx, cy, cx + cellW, cy + cellH, hovered ? 0x40FFFFFF : 0x30000000);
                drawContext.fill(cx, cy, cx + cellW, cy + cy + 1, 0x50FFFFFF);

                drawContext.drawCenteredTextWithShadow(textRenderer, labels[row][col], cx + cellW / 2, cy + cellH / 2 - 4, 0xFFCCCCCC);
            }
        }

        y += 3 * (cellH + 4) + 12;

        y = drawButton(drawContext, "Custom Position (Drag)", x, y, w, mouseX, mouseY, () -> {
            MinecraftClient.getInstance().setScreen(new HudPositionScreen(this));
        });
    }

    private int drawToggle(DrawContext drawContext, String label, boolean value, int x, int y, int w, int mouseX, int mouseY, Runnable onToggle) {
        int toggleW = 40;
        int toggleH = 20;
        int toggleX = x + w - toggleW;

        boolean hovered = mouseX >= toggleX && mouseX < toggleX + toggleW && mouseY >= y && mouseY < y + toggleH;
        if (hovered && wasClicked(mouseX, mouseY)) {
            onToggle.run();
        }

        drawContext.drawTextWithShadow(textRenderer, label, x, y + 6, 0xFFCCCCCC);

        int bgCol = value ? 0xFF4FC3F7 : 0xFF555555;
        int knobX = value ? toggleX + toggleW - 12 : toggleX + 2;

        drawContext.fill(toggleX, y + 2, toggleX + toggleW, y + toggleH - 2, bgCol);
        drawContext.fill(knobX, y + 3, knobX + 10, y + toggleH - 3, 0xFFFFFFFF);

        return y + WIDGET_HEIGHT + WIDGET_SPACING;
    }

    private int drawTextField(DrawContext drawContext, String text, int x, int y, int w, int mouseX, int mouseY) {
        int h = 20;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        if (hovered && wasClicked(mouseX, mouseY)) {
            formatFieldFocused = true;
            formatFieldCursor = text.length();
        } else if (wasClicked(mouseX, mouseY)) {
            formatFieldFocused = false;
        }

        drawContext.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        int borderColor = formatFieldFocused ? 0xFF4FC3F7 : 0xFF444444;
        drawContext.fill(x, y, x + w, y + 1, borderColor);
        drawContext.fill(x, y + h - 1, x + w, y + h, borderColor);
        drawContext.fill(x, y, x + 1, y + h, borderColor);
        drawContext.fill(x + w - 1, y, x + w, y + h, borderColor);

        String displayText = text;
        if (formatFieldFocused) {
            long time = System.currentTimeMillis();
            if ((time / 500) % 2 == 0) {
                displayText = text.substring(0, Math.min(formatFieldCursor, text.length())) + "|" + text.substring(Math.min(formatFieldCursor, text.length()));
            }
        }

        drawContext.drawTextWithShadow(textRenderer, displayText, x + 4, y + 6, 0xFFE0E0E0);

        return y + h + 2;
    }

    private int drawColorButton(DrawContext drawContext, int color, int x, int y, int w, int mouseX, int mouseY, java.util.function.IntConsumer onChange) {
        int h = 20;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        if (hovered && wasClicked(mouseX, mouseY)) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            r = (r + 40) % 256;
            g = (g + 40) % 256;
            b = (b + 40) % 256;

            onChange.accept((a << 24) | (r << 16) | (g << 8) | b);
        }

        int opaque = color | 0xFF000000;
        drawContext.fill(x, y, x + w, y + h, opaque);
        drawContext.fill(x, y, x + w, y + 1, 0xFF000000);
        drawContext.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        drawContext.fill(x, y, x + 1, y + h, 0xFF000000);
        drawContext.fill(x + w - 1, y, x + w, y + h, 0xFF000000);

        Color c = new Color(color, true);
        String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        drawContext.drawCenteredTextWithShadow(textRenderer, hex, x + w / 2, y + 6,
                (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) > 128 ? 0xFF000000 : 0xFFFFFFFF);

        return y + h + WIDGET_SPACING;
    }

    private int drawSlider(DrawContext drawContext, double value, double min, double max, double step, int x, int y, int w, int mouseX, int mouseY, java.util.function.DoubleConsumer onChange) {
        int h = 20;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean dragging = hovered && isMouseDown();

        if (dragging) {
            double ratio = (double) (mouseX - x) / w;
            double newVal = min + ratio * (max - min);
            newVal = Math.round(newVal / step) * step;
            newVal = Math.max(min, Math.min(max, newVal));
            onChange.accept(newVal);
        }

        drawContext.fill(x, y + 6, x + w, y + 14, 0xFF333333);

        double ratio = (value - min) / (max - min);
        int filledW = (int) (w * ratio);
        drawContext.fill(x, y + 6, x + filledW, y + 14, 0xFF4FC3F7);

        int knobX = x + filledW - 4;
        drawContext.fill(knobX, y + 3, knobX + 8, y + 17, 0xFFFFFFFF);

        return y + h + WIDGET_SPACING;
    }

    private int drawButton(DrawContext drawContext, String label, int x, int y, int w, int mouseX, int mouseY, Runnable onClick) {
        int h = 22;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        if (hovered && wasClicked(mouseX, mouseY)) {
            onClick.run();
        }

        int bg = hovered ? 0xFF3A3A3A : 0xFF2A2A2A;
        drawContext.fill(x, y, x + w, y + h, bg);
        drawContext.fill(x, y, x + w, y + 1, 0xFF555555);
        drawContext.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        drawContext.fill(x, y, x + 1, y + h, 0xFF555555);
        drawContext.fill(x + w - 1, y, x + w, y + h, 0xFF555555);

        drawContext.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 7, 0xFFE0E0E0);

        return y + h + WIDGET_SPACING;
    }

    private boolean wasClicked(int mouseX, int mouseY) {
        return isMouseDown();
    }

    private boolean isMouseDown() {
        return MinecraftClient.getInstance().mouse != null &&
                org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                        MinecraftClient.getInstance().getWindow().getHandle(),
                        org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    @Override
    public boolean mouseClicked(Click click, boolean always) {
        if (click.button() != 0) return super.mouseClicked(click, always);

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (mouseX < SIDEBAR_WIDTH && mouseY >= 24) {
            int tabIndex = (mouseY - 24) / 28;
            if (tabIndex >= 0 && tabIndex < 2) {
                selectedTab = tabIndex;
                return true;
            }
        }

        if (selectedTab == 1) {
            ModConfig config = ModConfig.getConfig();
            int contentX = SIDEBAR_WIDTH + PADDING;
            int contentY = 50;
            int contentWidth = width - SIDEBAR_WIDTH - PADDING * 2;
            int cellW = (contentWidth - 8) / 3;
            int cellH = 44;

            double[][] posXGrid = {{0.0, 0.5, 1.0}, {0.0, 0.5, 1.0}, {0.0, 0.5, 1.0}};
            double[][] posYGrid = {{0.0, 0.0, 0.0}, {0.5, 0.5, 0.5}, {1.0, 1.0, 1.0}};

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int cx = contentX + col * (cellW + 4);
                    int cy = contentY + row * (cellH + 4);
                    if (mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH) {
                        config.posX = posXGrid[row][col];
                        config.posY = posYGrid[row][col];
                        config.save();
                        return true;
                    }
                }
            }
        }

        int tfX = SIDEBAR_WIDTH + PADDING;
        int tfY = getTextFieldY();
        int tfW = width - SIDEBAR_WIDTH - PADDING * 2;
        if (mouseX >= tfX && mouseX < tfX + tfW && mouseY >= tfY && mouseY < tfY + 20) {
            formatFieldFocused = true;
        } else {
            formatFieldFocused = false;
        }

        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        if (formatFieldFocused) {
            int key = keyInput.key();
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (formatFieldCursor > 0 && formatFieldText.length() > 0) {
                    formatFieldText = formatFieldText.substring(0, formatFieldCursor - 1) + formatFieldText.substring(formatFieldCursor);
                    formatFieldCursor--;
                    syncFormatToConfig();
                }
                return true;
            } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
                if (formatFieldCursor > 0) formatFieldCursor--;
                return true;
            } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
                if (formatFieldCursor < formatFieldText.length()) formatFieldCursor++;
                return true;
            } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                formatFieldFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput charInput) {
        if (formatFieldFocused) {
            char c = (char) charInput.codepoint();
            if (c >= 32 && c != 127) {
                formatFieldText = formatFieldText.substring(0, formatFieldCursor) + c + formatFieldText.substring(formatFieldCursor);
                formatFieldCursor++;
                syncFormatToConfig();
                return true;
            }
        }
        return super.charTyped(charInput);
    }

    private void syncFormatToConfig() {
        ModConfig config = ModConfig.getConfig();
        config.displayFormat = formatFieldText;
        config.save();
    }

    private int getTextFieldY() {
        return 32 + (WIDGET_HEIGHT + WIDGET_SPACING) + 14;
    }

    @Override
    public void close() {
        ModConfig.getConfig().save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
