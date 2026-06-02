package com.notnawfas.betterpvputils;

import com.notnawfas.betterpvputils.hud.CpsHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class BetterPvPUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new CpsHudOverlay());
        BetterPvPUtils.LOGGER.info("Better PvP Utils client initialized");
    }
}
