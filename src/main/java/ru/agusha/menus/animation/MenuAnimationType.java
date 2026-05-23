package ru.agusha.menus.animation;

import java.util.Locale;

public enum MenuAnimationType {
    NONE,
    FILL,
    BORDER,
    ROWS,
    CENTER,
    SCRIPT;

    public static MenuAnimationType from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return MenuAnimationType.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
