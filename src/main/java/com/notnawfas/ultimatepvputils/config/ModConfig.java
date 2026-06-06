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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static ModConfig instance;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimatepvputils.json");
    private static final Path CONFIG_BACKUP_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimatepvputils.json.bak");
    private static final Path CONFIG_TEMP_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimatepvputils.json.tmp");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(CounterEntry.class, new CounterEntryAdapter())
        .registerTypeAdapter(ShieldConfig.class, new ShieldConfigAdapter())
        .create();

    public boolean enabled = true;
    public List<CounterEntry> counters = new ArrayList<>();
    public ShieldConfig shieldConfig = new ShieldConfig();

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
                    if (loaded.shieldConfig == null) loaded.shieldConfig = new ShieldConfig();
                    for (CounterEntry e : loaded.counters) e.validate();
                    loaded.shieldConfig.validate();
                    copyFrom(loaded);
                }
            } catch (Exception e) {
                UltimatePvPUtils.LOGGER.error("Failed to parse config, backing up and resetting", e);
                try {
                    Files.copy(CONFIG_PATH, CONFIG_BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
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

    private void copyFrom(ModConfig other) {
        this.enabled = other.enabled;
        this.counters.clear();
        this.counters.addAll(other.counters);
        this.shieldConfig = other.shieldConfig;
    }

    private volatile boolean dirty;

    public void save() {
        try {
            Files.writeString(CONFIG_TEMP_PATH, GSON.toJson(this));
            try {
                Files.move(CONFIG_TEMP_PATH, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(CONFIG_TEMP_PATH, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            UltimatePvPUtils.LOGGER.error("Failed to save config", e);
            try { Files.deleteIfExists(CONFIG_TEMP_PATH); } catch (IOException ignored) {}
        }
    }

    public void requestSave() {
        dirty = true;
    }

    public void flushIfDirty() {
        if (dirty) save();
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
                    case "backgroundColor" -> backgroundColor = readColor(in);
                    case "textColor" -> textColor = readColor(in);
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

        private int readColor(JsonReader in) throws IOException {
            try {
                return in.nextInt();
            } catch (IllegalStateException e) {
                String hex = in.nextString();
                try {
                    return (int) Long.parseLong(hex.replace("#", "").replace("0x", ""), 16);
                } catch (NumberFormatException ex) {
                    UltimatePvPUtils.LOGGER.warn("Invalid color value '{}', using default", hex);
                    return 0x80000000;
                }
            }
        }
    }

    private static class ShieldConfigAdapter extends com.google.gson.TypeAdapter<ShieldConfig> {
        @Override
        public void write(JsonWriter out, ShieldConfig cfg) throws IOException {
            out.beginObject();
            out.name("size").value(cfg.size);
            out.name("offsetX").value(cfg.offsetX);
            out.name("offsetY").value(cfg.offsetY);
            out.name("visible").value(cfg.visible);
            out.endObject();
        }

        @Override
        public ShieldConfig read(JsonReader in) throws IOException {
            double size = 1.0, offsetX = 0.0, offsetY = 0.0;
            boolean visible = true;

            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                switch (key) {
                    case "size" -> size = in.nextDouble();
                    case "offsetX" -> offsetX = in.nextDouble();
                    case "offsetY" -> offsetY = in.nextDouble();
                    case "visible" -> visible = in.nextBoolean();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return new ShieldConfig(size, offsetX, offsetY, visible);
        }
    }
}
