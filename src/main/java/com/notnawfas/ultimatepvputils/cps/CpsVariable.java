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
        // A-Z
        KEY_ALIASES.put("a", GLFW.GLFW_KEY_A);
        KEY_ALIASES.put("b", GLFW.GLFW_KEY_B);
        KEY_ALIASES.put("c", GLFW.GLFW_KEY_C);
        KEY_ALIASES.put("d", GLFW.GLFW_KEY_D);
        KEY_ALIASES.put("e", GLFW.GLFW_KEY_E);
        KEY_ALIASES.put("f", GLFW.GLFW_KEY_F);
        KEY_ALIASES.put("g", GLFW.GLFW_KEY_G);
        KEY_ALIASES.put("h", GLFW.GLFW_KEY_H);
        KEY_ALIASES.put("i", GLFW.GLFW_KEY_I);
        KEY_ALIASES.put("j", GLFW.GLFW_KEY_J);
        KEY_ALIASES.put("k", GLFW.GLFW_KEY_K);
        KEY_ALIASES.put("l", GLFW.GLFW_KEY_L);
        KEY_ALIASES.put("m", GLFW.GLFW_KEY_M);
        KEY_ALIASES.put("n", GLFW.GLFW_KEY_N);
        KEY_ALIASES.put("o", GLFW.GLFW_KEY_O);
        KEY_ALIASES.put("p", GLFW.GLFW_KEY_P);
        KEY_ALIASES.put("q", GLFW.GLFW_KEY_Q);
        KEY_ALIASES.put("r", GLFW.GLFW_KEY_R);
        KEY_ALIASES.put("s", GLFW.GLFW_KEY_S);
        KEY_ALIASES.put("t", GLFW.GLFW_KEY_T);
        KEY_ALIASES.put("u", GLFW.GLFW_KEY_U);
        KEY_ALIASES.put("v", GLFW.GLFW_KEY_V);
        KEY_ALIASES.put("w", GLFW.GLFW_KEY_W);
        KEY_ALIASES.put("x", GLFW.GLFW_KEY_X);
        KEY_ALIASES.put("y", GLFW.GLFW_KEY_Y);
        KEY_ALIASES.put("z", GLFW.GLFW_KEY_Z);

        // 0-9
        KEY_ALIASES.put("0", GLFW.GLFW_KEY_0);
        KEY_ALIASES.put("1", GLFW.GLFW_KEY_1);
        KEY_ALIASES.put("2", GLFW.GLFW_KEY_2);
        KEY_ALIASES.put("3", GLFW.GLFW_KEY_3);
        KEY_ALIASES.put("4", GLFW.GLFW_KEY_4);
        KEY_ALIASES.put("5", GLFW.GLFW_KEY_5);
        KEY_ALIASES.put("6", GLFW.GLFW_KEY_6);
        KEY_ALIASES.put("7", GLFW.GLFW_KEY_7);
        KEY_ALIASES.put("8", GLFW.GLFW_KEY_8);
        KEY_ALIASES.put("9", GLFW.GLFW_KEY_9);

        // Function keys
        KEY_ALIASES.put("f1", GLFW.GLFW_KEY_F1);
        KEY_ALIASES.put("f2", GLFW.GLFW_KEY_F2);
        KEY_ALIASES.put("f3", GLFW.GLFW_KEY_F3);
        KEY_ALIASES.put("f4", GLFW.GLFW_KEY_F4);
        KEY_ALIASES.put("f5", GLFW.GLFW_KEY_F5);
        KEY_ALIASES.put("f6", GLFW.GLFW_KEY_F6);
        KEY_ALIASES.put("f7", GLFW.GLFW_KEY_F7);
        KEY_ALIASES.put("f8", GLFW.GLFW_KEY_F8);
        KEY_ALIASES.put("f9", GLFW.GLFW_KEY_F9);
        KEY_ALIASES.put("f10", GLFW.GLFW_KEY_F10);
        KEY_ALIASES.put("f11", GLFW.GLFW_KEY_F11);
        KEY_ALIASES.put("f12", GLFW.GLFW_KEY_F12);
        KEY_ALIASES.put("f13", GLFW.GLFW_KEY_F13);
        KEY_ALIASES.put("f14", GLFW.GLFW_KEY_F14);
        KEY_ALIASES.put("f15", GLFW.GLFW_KEY_F15);
        KEY_ALIASES.put("f16", GLFW.GLFW_KEY_F16);
        KEY_ALIASES.put("f17", GLFW.GLFW_KEY_F17);
        KEY_ALIASES.put("f18", GLFW.GLFW_KEY_F18);
        KEY_ALIASES.put("f19", GLFW.GLFW_KEY_F19);
        KEY_ALIASES.put("f20", GLFW.GLFW_KEY_F20);
        KEY_ALIASES.put("f21", GLFW.GLFW_KEY_F21);
        KEY_ALIASES.put("f22", GLFW.GLFW_KEY_F22);
        KEY_ALIASES.put("f23", GLFW.GLFW_KEY_F23);
        KEY_ALIASES.put("f24", GLFW.GLFW_KEY_F24);
        KEY_ALIASES.put("f25", GLFW.GLFW_KEY_F25);

        // Numpad digits
        KEY_ALIASES.put("numpad0", GLFW.GLFW_KEY_KP_0);
        KEY_ALIASES.put("numpad1", GLFW.GLFW_KEY_KP_1);
        KEY_ALIASES.put("numpad2", GLFW.GLFW_KEY_KP_2);
        KEY_ALIASES.put("numpad3", GLFW.GLFW_KEY_KP_3);
        KEY_ALIASES.put("numpad4", GLFW.GLFW_KEY_KP_4);
        KEY_ALIASES.put("numpad5", GLFW.GLFW_KEY_KP_5);
        KEY_ALIASES.put("numpad6", GLFW.GLFW_KEY_KP_6);
        KEY_ALIASES.put("numpad7", GLFW.GLFW_KEY_KP_7);
        KEY_ALIASES.put("numpad8", GLFW.GLFW_KEY_KP_8);
        KEY_ALIASES.put("numpad9", GLFW.GLFW_KEY_KP_9);

        // Numpad operators
        KEY_ALIASES.put("numpad_add", GLFW.GLFW_KEY_KP_ADD);
        KEY_ALIASES.put("numpad_subtract", GLFW.GLFW_KEY_KP_SUBTRACT);
        KEY_ALIASES.put("numpad_multiply", GLFW.GLFW_KEY_KP_MULTIPLY);
        KEY_ALIASES.put("numpad_divide", GLFW.GLFW_KEY_KP_DIVIDE);
        KEY_ALIASES.put("numpad_decimal", GLFW.GLFW_KEY_KP_DECIMAL);
        KEY_ALIASES.put("numpad_enter", GLFW.GLFW_KEY_KP_ENTER);
        KEY_ALIASES.put("numpad_equal", GLFW.GLFW_KEY_KP_EQUAL);

        // Navigation
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
        KEY_ALIASES.put("print_screen", GLFW.GLFW_KEY_PRINT_SCREEN);
        KEY_ALIASES.put("scroll_lock", GLFW.GLFW_KEY_SCROLL_LOCK);
        KEY_ALIASES.put("pause", GLFW.GLFW_KEY_PAUSE);

        // Modifiers
        KEY_ALIASES.put("shift", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_ALIASES.put("left_shift", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_ALIASES.put("right_shift", GLFW.GLFW_KEY_RIGHT_SHIFT);
        KEY_ALIASES.put("ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_ALIASES.put("left_ctrl", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_ALIASES.put("right_ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL);
        KEY_ALIASES.put("alt", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_ALIASES.put("left_alt", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_ALIASES.put("right_alt", GLFW.GLFW_KEY_RIGHT_ALT);
        KEY_ALIASES.put("super", GLFW.GLFW_KEY_LEFT_SUPER);
        KEY_ALIASES.put("left_super", GLFW.GLFW_KEY_LEFT_SUPER);
        KEY_ALIASES.put("right_super", GLFW.GLFW_KEY_RIGHT_SUPER);
        KEY_ALIASES.put("caps_lock", GLFW.GLFW_KEY_CAPS_LOCK);
        KEY_ALIASES.put("num_lock", GLFW.GLFW_KEY_NUM_LOCK);

        // Special keys
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
        KEY_ALIASES.put("world1", GLFW.GLFW_KEY_WORLD_1);
        KEY_ALIASES.put("world2", GLFW.GLFW_KEY_WORLD_2);
        KEY_ALIASES.put("menu", GLFW.GLFW_KEY_MENU);
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
            return "keyboard." + key;
        }
    }
}
