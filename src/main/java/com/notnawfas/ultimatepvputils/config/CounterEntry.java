package com.notnawfas.ultimatepvputils.config;

public class CounterEntry {

    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;
    private static final double POS_EPSILON = 0.01;

    public static final String[] PRESETS = {
        "Top-Left", "Top-Center", "Top-Right",
        "Bottom-Left", "Bottom-Center", "Bottom-Right",
        "Center", "Custom"
    };

    public String name;
    public String displayFormat;
    public boolean showBackground;
    public int backgroundColor;
    public int textColor;
    public double scale;
    public double posX;
    public double posY;
    public String presetName;

    public CounterEntry(String name, String displayFormat, boolean showBackground,
                        int backgroundColor, int textColor, double scale,
                        double posX, double posY, String presetName) {
        this.name = safeName(name);
        this.displayFormat = displayFormat != null ? displayFormat : "";
        this.showBackground = showBackground;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.scale = clampScale(scale);
        this.posX = clampPos(posX);
        this.posY = clampPos(posY);
        this.presetName = presetName != null ? presetName : "Top-Left";
    }

    public void validate() {
        this.name = safeName(this.name);
        this.displayFormat = this.displayFormat != null ? this.displayFormat : "";
        this.scale = clampScale(this.scale);
        this.posX = clampPos(this.posX);
        this.posY = clampPos(this.posY);
        this.presetName = this.presetName != null ? this.presetName : "Top-Left";
    }

    public float clampedScale() {
        return (float) clampScale(this.scale);
    }

    public void applyPreset(String name) {
        this.presetName = name;
        switch (name) {
            case "Top-Left"      -> { posX = 0.0; posY = 0.0; }
            case "Top-Center"    -> { posX = 0.5; posY = 0.0; }
            case "Top-Right"     -> { posX = 1.0; posY = 0.0; }
            case "Bottom-Left"   -> { posX = 0.0; posY = 1.0; }
            case "Bottom-Center" -> { posX = 0.5; posY = 1.0; }
            case "Bottom-Right"  -> { posX = 1.0; posY = 1.0; }
            case "Center"        -> { posX = 0.5; posY = 0.5; }
            case "Custom"        -> {}
        }
    }

    public boolean isCustomPosition() {
        return "Custom".equals(presetName);
    }

    public String getNextPreset() {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i].equals(presetName)) return PRESETS[(i + 1) % PRESETS.length];
        }
        return "Top-Left";
    }

    public static String detectPreset(double posX, double posY) {
        if (near(posX, 0.0) && near(posY, 0.0)) return "Top-Left";
        if (near(posX, 0.5) && near(posY, 0.0)) return "Top-Center";
        if (near(posX, 1.0) && near(posY, 0.0)) return "Top-Right";
        if (near(posX, 0.0) && near(posY, 1.0)) return "Bottom-Left";
        if (near(posX, 0.5) && near(posY, 1.0)) return "Bottom-Center";
        if (near(posX, 1.0) && near(posY, 1.0)) return "Bottom-Right";
        if (near(posX, 0.5) && near(posY, 0.5)) return "Center";
        return "Custom";
    }

    private static String safeName(String s) {
        return (s != null && !s.isBlank()) ? s : "Counter";
    }

    private static double clampScale(double v) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, v)); }
    private static double clampPos(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    private static boolean near(double a, double b) { return Math.abs(a - b) < POS_EPSILON; }
}
