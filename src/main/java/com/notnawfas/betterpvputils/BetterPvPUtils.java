package com.notnawfas.betterpvputils;

import com.notnawfas.betterpvputils.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterPvPUtils implements ModInitializer {

    public static final String MOD_ID = "betterpvputils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.init();
        LOGGER.info("Better PvP Utils initialized");
    }
}
