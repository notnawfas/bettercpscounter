package com.notnawfas.ultimatepvputils;

import com.notnawfas.ultimatepvputils.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimatePvPUtils implements ModInitializer {

    public static final String MOD_ID = "ultimatepvputils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_VERSION = "@VERSION@";

    @Override
    public void onInitialize() {
        ModConfig.init();
        LOGGER.info("Ultimate PvP Utils v{} initialized", MOD_VERSION);
    }
}
