package com.notnawfas.ultimatepvputils.cps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CpsTracker {

    public static final CpsTracker INSTANCE = new CpsTracker();

    private static final long WINDOW_MS = 1000L;
    private static final int MAX_ENTRIES_PER_KEY = 200;
    private static final int EVICTION_INTERVAL_MS = 30000;

    private final Map<String, Deque<Long>> timestamps = new HashMap<>();
    private long lastEviction = System.currentTimeMillis();

    private CpsTracker() {
    }

    public void onMouseClick(int button) {
        String key = "mouse." + (button == 0 ? "left" : button == 1 ? "right" : button);
        record(key);
    }

    public void onKeyPress(int keyCode) {
        record("keyboard." + keyCode);
    }

    public void onKeyPress(int keyCode, int action) {
        if (action == 1) {
            record("keyboard." + keyCode);
        }
    }

    public int getCps(String inputId) {
        Deque<Long> deque = timestamps.get(inputId);
        if (deque == null) return 0;
        long now = System.currentTimeMillis();
        evictOld(deque, now);
        return deque.size();
    }

    private void record(String key) {
        Deque<Long> deque = timestamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.addLast(System.currentTimeMillis());
        if (deque.size() > MAX_ENTRIES_PER_KEY) {
            deque.pollFirst();
        }
        maybeEvictAll();
    }

    private void evictOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MS) {
            deque.pollFirst();
        }
    }

    private void maybeEvictAll() {
        long now = System.currentTimeMillis();
        if (now - lastEviction < EVICTION_INTERVAL_MS) return;
        lastEviction = now;
        timestamps.entrySet().removeIf(entry -> {
            evictOld(entry.getValue(), now);
            return entry.getValue().isEmpty();
        });
    }
}
