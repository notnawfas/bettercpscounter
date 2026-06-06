package com.notnawfas.ultimatepvputils.cps;

import java.util.HashMap;
import java.util.Map;

public class CpsTracker {

    public static final CpsTracker INSTANCE = new CpsTracker();

    private static final long WINDOW_MS = 1000L;
    private static final int RING_SIZE = 256;
    private static final int EVICTION_INTERVAL_MS = 30000;

    private static final class RingBuffer {
        final long[] timestamps = new long[RING_SIZE];
        int head = 0;
        int count = 0;

        void push(long ts) {
            timestamps[(head + count) % RING_SIZE] = ts;
            if (count < RING_SIZE) {
                count++;
            } else {
                head = (head + 1) % RING_SIZE;
            }
        }

        int sizeAfter(long now) {
            while (count > 0 && now - timestamps[head] >= WINDOW_MS) {
                head = (head + 1) % RING_SIZE;
                count--;
            }
            return count;
        }

        boolean isEmptyAfter(long now) {
            return sizeAfter(now) == 0;
        }
    }

    private final Map<String, RingBuffer> buffers = new HashMap<>();
    private long lastEviction = System.currentTimeMillis();

    private CpsTracker() {}

    public void onMouseClick(int button, int action) {
        if (action != 1) return;
        String key = "mouse." + (button == 0 ? "left" : button == 1 ? "right" : button);
        record(key);
    }

    public void onKeyPress(int keyCode, int action) {
        if (action == 1) {
            record("keyboard." + keyCode);
        }
    }

    public int getCps(String inputId) {
        RingBuffer buf = buffers.get(inputId);
        if (buf == null) return 0;
        return buf.sizeAfter(System.currentTimeMillis());
    }

    private void record(String key) {
        buffers.computeIfAbsent(key, k -> new RingBuffer()).push(System.currentTimeMillis());
        maybeEvictAll();
    }

    private void maybeEvictAll() {
        long now = System.currentTimeMillis();
        if (now - lastEviction < EVICTION_INTERVAL_MS) return;
        lastEviction = now;
        buffers.entrySet().removeIf(entry -> entry.getValue().isEmptyAfter(now));
    }
}
