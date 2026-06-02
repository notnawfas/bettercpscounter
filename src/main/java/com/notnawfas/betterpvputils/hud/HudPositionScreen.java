package com.notnawfas.betterpvputils.hud;

import com.notnawfas.betterpvputils.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;

public class HudPositionScreen extends Screen {

    private static final int PADDING = 2;

    private final Screen parent;
    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public HudPositionScreen(Screen parent) {
        super(Text.translatable("betterpvputils.screen.reposition.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(width / 2 - 50, height - 30, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        renderBackground(drawContext, mouseX, mouseY, delta);

        ModConfig config = ModConfig.getConfig();

        int hudWidth = CpsHudOverlay.getHudWidth(config);
        int hudHeight = CpsHudOverlay.getHudHeight();

        int hudX = (int) (config.posX * (width - hudWidth));
        int hudY = (int) (config.posY * (height - hudHeight));

        hudX = Math.max(0, Math.min(hudX, width - hudWidth));
        hudY = Math.max(0, Math.min(hudY, height - hudHeight));

        CpsHudOverlay.renderPreview(drawContext, config, hudX, hudY);

        drawOutline(drawContext, hudX, hudY, hudWidth, hudHeight, 0xFFFFFFFF);

        drawContext.drawCenteredTextWithShadow(textRenderer, Text.translatable("betterpvputils.screen.reposition.title"), width / 2, 10, 0xFFFFFF);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void drawOutline(DrawContext drawContext, int x, int y, int width, int height, int color) {
        drawContext.fill(x, y, x + width, y + 1, color);
        drawContext.fill(x, y + height - 1, x + width, y + height, color);
        drawContext.fill(x, y, x + 1, y + height, color);
        drawContext.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean mouseClicked(Click click) {
        if (click.type() != Click.Type.MOUSE_LEFT) {
            return super.mouseClicked(click);
        }

        double mouseX = click.x();
        double mouseY = click.y();

        ModConfig config = ModConfig.getConfig();

        int hudWidth = CpsHudOverlay.getHudWidth(config);
        int hudHeight = CpsHudOverlay.getHudHeight();

        int hudX = (int) (config.posX * (width - hudWidth));
        int hudY = (int) (config.posY * (height - hudHeight));

        hudX = Math.max(0, Math.min(hudX, width - hudWidth));
        hudY = Math.max(0, Math.min(hudY, height - hudHeight));

        if (mouseX >= hudX && mouseX <= hudX + hudWidth && mouseY >= hudY && mouseY <= hudY + hudHeight) {
            dragging = true;
            dragOffsetX = mouseX - hudX;
            dragOffsetY = mouseY - hudY;
            return true;
        }
        return super.mouseClicked(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (dragging) {
            double mouseX = click.x();
            double mouseY = click.y();

            ModConfig config = ModConfig.getConfig();

            int hudWidth = CpsHudOverlay.getHudWidth(config);
            int hudHeight = CpsHudOverlay.getHudHeight();

            int newHudX = (int) (mouseX - dragOffsetX);
            int newHudY = (int) (mouseY - dragOffsetY);

            newHudX = Math.max(0, Math.min(newHudX, width - hudWidth));
            newHudY = Math.max(0, Math.min(newHudY, height - hudHeight));

            config.posX = (width - hudWidth) > 0 ? (double) newHudX / (width - hudWidth) : 0.0;
            config.posY = (height - hudHeight) > 0 ? (double) newHudY / (height - hudHeight) : 0.0;

            config.posX = Math.max(0.0, Math.min(1.0, config.posX));
            config.posY = Math.max(0.0, Math.min(1.0, config.posY));

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
        return super.mouseReleased(click);
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
