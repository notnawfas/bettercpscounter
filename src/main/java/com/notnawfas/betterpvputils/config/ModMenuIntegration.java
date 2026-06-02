package com.notnawfas.betterpvputils.config;

import com.notnawfas.betterpvputils.hud.HudPositionScreen;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import java.awt.Color;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> buildConfigScreen(parentScreen);
    }

    private Screen buildConfigScreen(Screen parent) {
        ModConfig config = ModConfig.getConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("betterpvputils.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("betterpvputils.config.category.cps"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.enabled"))
                                .description(OptionDescription.of(Text.translatable("betterpvputils.config.option.enabled.description")))
                                .binding(true, () -> config.enabled, val -> config.enabled = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.displayFormat"))
                                .description(OptionDescription.of(Text.translatable("betterpvputils.config.option.displayFormat.description")))
                                .binding("[LMB: %cps_mouse.left% | RMB: %cps_mouse.right%]", () -> config.displayFormat, val -> config.displayFormat = val)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.useMinecraftFont"))
                                .binding(true, () -> config.useMinecraftFont, val -> config.useMinecraftFont = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.showBackground"))
                                .binding(true, () -> config.showBackground, val -> config.showBackground = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.backgroundColor"))
                                .binding(new Color(0, 0, 0, 128), () -> new Color(config.backgroundColor, true), val -> config.backgroundColor = val.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .option(Option.<Color>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.textColor"))
                                .binding(Color.WHITE, () -> new Color(config.textColor, true), val -> config.textColor = val.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("betterpvputils.config.category.position"))
                        .option(Option.<Double>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.posX"))
                                .binding(0.0, () -> config.posX, val -> config.posX = val)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.005))
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.translatable("betterpvputils.config.option.posY"))
                                .binding(0.0, () -> config.posY, val -> config.posY = val)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(0.0, 1.0).step(0.005))
                                .build())
                        .build())
                .save(() -> ModConfig.getConfig().save())
                .build()
                .generateScreen(parent);
    }
}
