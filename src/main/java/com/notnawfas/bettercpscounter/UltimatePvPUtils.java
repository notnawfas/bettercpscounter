package com.notnawfas.bettercpscounter;

import com.notnawfas.bettercpscounter.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimatePvPUtils implements ModInitializer {

    public static final String MOD_ID = "bettercpscounter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_VERSION = "@VERSION@";

    @Override
    public void onInitialize() {
        ModConfig.init();
        LOGGER.info("Better CPS Counter v{} initialized", MOD_VERSION);
    }
}
