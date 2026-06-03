package com.notnawfas.ultimatepvputils.cps;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CpsVariable {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("%cps_(mouse|keyboard)\\.([a-zA-Z0-9_]+)%");

    private static final Map<String, Integer> KEY_ALIASES = new HashMap<>();

    static {
        KEY_ALIASES.put("w", GLFW.GLFW_KEY_W);
        KEY_ALIASES.put("a", GLFW.GLFW_KEY_A);
        KEY_ALIASES.put("s", GLFW.GLFW_KEY_S);
        KEY_ALIASES.put("d", GLFW.GLFW_KEY_D);
        KEY_ALIASES.put("space", GLFW.GLFW_KEY_SPACE);
        KEY_ALIASES.put("shift", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_ALIASES.put("ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_ALIASES.put("alt", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_ALIASES.put("tab", GLFW.GLFW_KEY_TAB);
        KEY_ALIASES.put("enter", GLFW.GLFW_KEY_ENTER);
        KEY_ALIASES.put("escape", GLFW.GLFW_KEY_ESCAPE);
        KEY_ALIASES.put("backspace", GLFW.GLFW_KEY_BACKSPACE);
        KEY_ALIASES.put("left", GLFW.GLFW_KEY_LEFT);
        KEY_ALIASES.put("right", GLFW.GLFW_KEY_RIGHT);
        KEY_ALIASES.put("up", GLFW.GLFW_KEY_UP);
        KEY_ALIASES.put("down", GLFW.GLFW_KEY_DOWN);
        KEY_ALIASES.put("e", GLFW.GLFW_KEY_E);
        KEY_ALIASES.put("q", GLFW.GLFW_KEY_Q);
        KEY_ALIASES.put("f", GLFW.GLFW_KEY_F);
        KEY_ALIASES.put("r", GLFW.GLFW_KEY_R);
        KEY_ALIASES.put("c", GLFW.GLFW_KEY_C);
        KEY_ALIASES.put("x", GLFW.GLFW_KEY_X);
        KEY_ALIASES.put("z", GLFW.GLFW_KEY_Z);
        KEY_ALIASES.put("1", GLFW.GLFW_KEY_1);
        KEY_ALIASES.put("2", GLFW.GLFW_KEY_2);
        KEY_ALIASES.put("3", GLFW.GLFW_KEY_3);
        KEY_ALIASES.put("4", GLFW.GLFW_KEY_4);
        KEY_ALIASES.put("5", GLFW.GLFW_KEY_5);
        KEY_ALIASES.put("6", GLFW.GLFW_KEY_6);
        KEY_ALIASES.put("7", GLFW.GLFW_KEY_7);
        KEY_ALIASES.put("8", GLFW.GLFW_KEY_8);
        KEY_ALIASES.put("9", GLFW.GLFW_KEY_9);
        KEY_ALIASES.put("0", GLFW.GLFW_KEY_0);
    }

    public static String resolve(String format) {
        Matcher matcher = TOKEN_PATTERN.matcher(format);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String type = matcher.group(1);
            String key = matcher.group(2);
            String inputId = resolveInputId(type, key);
            int cps = CpsTracker.INSTANCE.getCps(inputId);
            matcher.appendReplacement(result, String.valueOf(cps));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String resolveInputId(String type, String key) {
        if (type.equals("mouse")) {
            return "mouse." + key;
        }
        Integer keyCode = KEY_ALIASES.get(key.toLowerCase());
        if (keyCode != null) {
            return "keyboard." + keyCode;
        }
        try {
            int parsed = Integer.parseInt(key);
            return "keyboard." + parsed;
        } catch (NumberFormatException e) {
            return "keyboard." + key;
        }
    }
}
