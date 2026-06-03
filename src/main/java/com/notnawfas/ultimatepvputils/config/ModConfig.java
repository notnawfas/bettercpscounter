package com.notnawfas.ultimatepvputils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.notnawfas.ultimatepvputils.UltimatePvPUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static ModConfig instance;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimatepvputils.json");
    private static final Path CONFIG_BACKUP_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimatepvputils.json.bak");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CounterEntry.class, new CounterEntryAdapter())
            .create();

    public boolean enabled = true;
    public List<CounterEntry> counters = new ArrayList<>();

    public ModConfig() {
        counters.add(new CounterEntry(
                "CPS Counter",
                "[LMB: %cps_mouse.left% | RMB: %cps_mouse.right%]",
                true, 0x80000000, 0xFFFFFFFF, 1.0, 0.0, 0.0, "Top-Left"
        ));
    }

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
                    if (loaded.counters == null) loaded.counters = new ArrayList<>();
                    instance = loaded;
                }
            } catch (Exception e) {
                UltimatePvPUtils.LOGGER.error("Failed to parse config, backing up and resetting", e);
                try {
                    Files.copy(CONFIG_PATH, CONFIG_BACKUP_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    UltimatePvPUtils.LOGGER.info("Corrupt config backed up to {}", CONFIG_BACKUP_PATH);
                } catch (IOException backupEx) {
                    UltimatePvPUtils.LOGGER.error("Failed to backup corrupt config", backupEx);
                }
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
            UltimatePvPUtils.LOGGER.error("Failed to save config", e);
        }
    }

    private static class CounterEntryAdapter extends com.google.gson.TypeAdapter<CounterEntry> {
        @Override
        public void write(JsonWriter out, CounterEntry entry) throws IOException {
            out.beginObject();
            out.name("name").value(entry.name);
            out.name("displayFormat").value(entry.displayFormat);
            out.name("showBackground").value(entry.showBackground);
            out.name("backgroundColor").value(entry.backgroundColor);
            out.name("textColor").value(entry.textColor);
            out.name("scale").value(entry.scale);
            out.name("posX").value(entry.posX);
            out.name("posY").value(entry.posY);
            out.name("presetName").value(entry.presetName);
            out.endObject();
        }

        @Override
        public CounterEntry read(JsonReader in) throws IOException {
            String name = "Counter";
            String displayFormat = "[LMB: %cps_mouse.left% | RMB: %cps_mouse.right%]";
            boolean showBackground = true;
            int backgroundColor = 0x80000000;
            int textColor = 0xFFFFFFFF;
            double scale = 1.0;
            double posX = 0.0;
            double posY = 0.0;
            String presetName = "Top-Left";

            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                switch (key) {
                    case "name" -> name = in.nextString();
                    case "displayFormat" -> displayFormat = in.nextString();
                    case "showBackground" -> showBackground = in.nextBoolean();
                    case "backgroundColor" -> backgroundColor = in.nextInt();
                    case "textColor" -> textColor = in.nextInt();
                    case "scale" -> scale = in.nextDouble();
                    case "posX" -> posX = in.nextDouble();
                    case "posY" -> posY = in.nextDouble();
                    case "presetName" -> presetName = in.nextString();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return new CounterEntry(name, displayFormat, showBackground, backgroundColor, textColor, scale, posX, posY, presetName);
        }
    }
}
