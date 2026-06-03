package com.notnawfas.ultimatepvputils.cps;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class CpsTracker {

    public static final CpsTracker INSTANCE = new CpsTracker();

    private static final long WINDOW_MS = 1000L;

    private final Map<String, LinkedList<Long>> timestamps = new HashMap<>();

    private CpsTracker() {
    }

    public void onMouseClick(int button) {
        String key = "mouse." + (button == 0 ? "left" : button == 1 ? "right" : button);
        record(key);
    }

    public void onKeyPress(int keyCode) {
        record("keyboard." + keyCode);
    }

    public int getCps(String inputId) {
        LinkedList<Long> list = timestamps.get(inputId);
        if (list == null) return 0;
        long now = System.currentTimeMillis();
        list.removeIf(ts -> now - ts >= WINDOW_MS);
        return list.size();
    }

    private void record(String key) {
        timestamps.computeIfAbsent(key, k -> new LinkedList<>()).add(System.currentTimeMillis());
    }
}
