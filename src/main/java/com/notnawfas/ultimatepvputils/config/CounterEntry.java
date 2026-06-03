package com.notnawfas.ultimatepvputils.config;

public class CounterEntry {

    public String name;
    public String displayFormat;
    public boolean showBackground;
    public int backgroundColor;
    public int textColor;
    public double scale;
    public double posX;
    public double posY;
    public String presetName;

    public CounterEntry(String name, String displayFormat, boolean showBackground, int backgroundColor, int textColor, double scale, double posX, double posY, String presetName) {
        this.name = name;
        this.displayFormat = displayFormat;
        this.showBackground = showBackground;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.scale = scale;
        this.posX = posX;
        this.posY = posY;
        this.presetName = presetName;
    }

    public void applyPreset(String name) {
        this.presetName = name;
        switch (name) {
            case "Top-Left" -> { posX = 0.0; posY = 0.0; }
            case "Top-Right" -> { posX = 1.0; posY = 0.0; }
            case "Bottom-Left" -> { posX = 0.0; posY = 1.0; }
            case "Bottom-Right" -> { posX = 1.0; posY = 1.0; }
            case "Custom" -> {}
        }
    }

    public boolean isCustomPosition() {
        return "Custom".equals(presetName);
    }
}
