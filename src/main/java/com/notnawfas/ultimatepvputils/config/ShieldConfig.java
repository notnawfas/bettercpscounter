package com.notnawfas.ultimatepvputils.config;

public class ShieldConfig {

    public static final double MIN_SIZE = 0.1;
    public static final double MAX_SIZE = 5.0;
    public static final double MIN_OFFSET = 0.0;
    public static final double MAX_OFFSET = 100.0;

    public double size;
    public double offsetX;
    public double offsetY;
    public boolean visible;

    public ShieldConfig() {
        this(1.0, 0.0, 0.0, true);
    }

    public ShieldConfig(double size, double offsetX, double offsetY, boolean visible) {
        this.size = clampSize(size);
        this.offsetX = clampOffset(offsetX);
        this.offsetY = clampOffset(offsetY);
        this.visible = visible;
    }

    public void validate() {
        this.size = clampSize(this.size);
        this.offsetX = clampOffset(this.offsetX);
        this.offsetY = clampOffset(this.offsetY);
    }

    public void resetToDefaults() {
        this.size = 1.0;
        this.offsetX = 0.0;
        this.offsetY = 0.0;
        this.visible = true;
    }

    private static double clampSize(double v) { return Math.max(MIN_SIZE, Math.min(MAX_SIZE, v)); }
    private static double clampOffset(double v) { return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, v)); }
}
