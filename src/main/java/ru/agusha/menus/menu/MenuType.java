package ru.agusha.menus.menu;

import java.util.Locale;

public enum MenuType {
    CHEST,
    BOOK;

    public static MenuType from(String value) {
        if (value == null || value.isBlank()) {
            return CHEST;
        }
        try {
            return MenuType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CHEST;
        }
    }
}
