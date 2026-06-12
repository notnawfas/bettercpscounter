package com.notnawfas.bettercpscounter.cps;

import com.notnawfas.bettercpscounter.UltimatePvPUtils;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CpsVariable {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("%cps_(mouse|keyboard)\\.([a-zA-Z0-9_]+)%");

    private static final Map<String, Integer> KEY_ALIASES = new HashMap<>();

    static {
        for (char c = 'a'; c <= 'z'; c++) KEY_ALIASES.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'a'));
        for (char c = '0'; c <= '9'; c++) KEY_ALIASES.put(String.valueOf(c), GLFW.GLFW_KEY_0 + (c - '0'));

        for (int i = 1; i <= 12; i++) KEY_ALIASES.put("f" + i, GLFW.GLFW_KEY_F1 + (i - 1));

        for (int i = 0; i <= 9; i++) KEY_ALIASES.put("numpad" + i, GLFW.GLFW_KEY_KP_0 + i);
        KEY_ALIASES.put("numpad_add", GLFW.GLFW_KEY_KP_ADD);
        KEY_ALIASES.put("numpad_subtract", GLFW.GLFW_KEY_KP_SUBTRACT);
        KEY_ALIASES.put("numpad_multiply", GLFW.GLFW_KEY_KP_MULTIPLY);
        KEY_ALIASES.put("numpad_divide", GLFW.GLFW_KEY_KP_DIVIDE);
        KEY_ALIASES.put("numpad_decimal", GLFW.GLFW_KEY_KP_DECIMAL);
        KEY_ALIASES.put("numpad_enter", GLFW.GLFW_KEY_KP_ENTER);

        KEY_ALIASES.put("up", GLFW.GLFW_KEY_UP);
        KEY_ALIASES.put("down", GLFW.GLFW_KEY_DOWN);
        KEY_ALIASES.put("left", GLFW.GLFW_KEY_LEFT);
        KEY_ALIASES.put("right", GLFW.GLFW_KEY_RIGHT);
        KEY_ALIASES.put("insert", GLFW.GLFW_KEY_INSERT);
        KEY_ALIASES.put("delete", GLFW.GLFW_KEY_DELETE);
        KEY_ALIASES.put("home", GLFW.GLFW_KEY_HOME);
        KEY_ALIASES.put("end", GLFW.GLFW_KEY_END);
        KEY_ALIASES.put("page_up", GLFW.GLFW_KEY_PAGE_UP);
        KEY_ALIASES.put("page_down", GLFW.GLFW_KEY_PAGE_DOWN);

        KEY_ALIASES.put("shift", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_ALIASES.put("left_shift", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_ALIASES.put("right_shift", GLFW.GLFW_KEY_RIGHT_SHIFT);
        KEY_ALIASES.put("ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_ALIASES.put("left_ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_ALIASES.put("right_ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL);
        KEY_ALIASES.put("alt", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_ALIASES.put("left_alt", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_ALIASES.put("right_alt", GLFW.GLFW_KEY_RIGHT_ALT);
        KEY_ALIASES.put("caps_lock", GLFW.GLFW_KEY_CAPS_LOCK);

        KEY_ALIASES.put("space", GLFW.GLFW_KEY_SPACE);
        KEY_ALIASES.put("enter", GLFW.GLFW_KEY_ENTER);
        KEY_ALIASES.put("escape", GLFW.GLFW_KEY_ESCAPE);
        KEY_ALIASES.put("tab", GLFW.GLFW_KEY_TAB);
        KEY_ALIASES.put("backspace", GLFW.GLFW_KEY_BACKSPACE);
        KEY_ALIASES.put("grave", GLFW.GLFW_KEY_GRAVE_ACCENT);
        KEY_ALIASES.put("tilde", GLFW.GLFW_KEY_GRAVE_ACCENT);
        KEY_ALIASES.put("minus", GLFW.GLFW_KEY_MINUS);
        KEY_ALIASES.put("equal", GLFW.GLFW_KEY_EQUAL);
        KEY_ALIASES.put("left_bracket", GLFW.GLFW_KEY_LEFT_BRACKET);
        KEY_ALIASES.put("right_bracket", GLFW.GLFW_KEY_RIGHT_BRACKET);
        KEY_ALIASES.put("backslash", GLFW.GLFW_KEY_BACKSLASH);
        KEY_ALIASES.put("semicolon", GLFW.GLFW_KEY_SEMICOLON);
        KEY_ALIASES.put("apostrophe", GLFW.GLFW_KEY_APOSTROPHE);
        KEY_ALIASES.put("comma", GLFW.GLFW_KEY_COMMA);
        KEY_ALIASES.put("period", GLFW.GLFW_KEY_PERIOD);
        KEY_ALIASES.put("slash", GLFW.GLFW_KEY_SLASH);
    }

    public static final String DOCS_URL = "https://modrinth.com/project/better-cps-counter";

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
            if ("middle".equalsIgnoreCase(key)) return "mouse.2";
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
            UltimatePvPUtils.LOGGER.warn("Unknown key alias '{}' in format token — use numeric GLFW key code instead (e.g. %cps_keyboard.65% for A)", key);
            return "keyboard.-1";
        }
    }
}
