package com.notnawfas.betterpvputils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static ModConfig instance;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("betterpvputils.json5");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public String displayFormat = "[LMB: %cps_mouse.left% | RMB: %cps_mouse.right%]";
    public boolean useMinecraftFont = true;
    public boolean showBackground = true;
    public int backgroundColor = 0x80000000;
    public int textColor = 0xFFFFFFFF;
    public double posX = 0.0;
    public double posY = 0.0;

    public static ModConfig getConfig() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public static void init() {
        getConfig().load();
    }

    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                save();
            }
        } else {
            save();
        }
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
