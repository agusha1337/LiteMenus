package ru.agusha.menus.menu;

import java.util.Locale;
import org.bukkit.event.inventory.ClickType;

public enum MenuClickType {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    MIDDLE,
    DROP,
    NUMBER_KEY,
    ANY;

    public static MenuClickType fromConfig(String value) {
        String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return MenuClickType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return ANY;
        }
    }

    public static MenuClickType fromBukkit(ClickType type) {
        return switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SHIFT_LEFT -> SHIFT_LEFT;
            case SHIFT_RIGHT -> SHIFT_RIGHT;
            case MIDDLE -> MIDDLE;
            case DROP, CONTROL_DROP -> DROP;
            case NUMBER_KEY -> NUMBER_KEY;
            default -> ANY;
        };
    }
}
