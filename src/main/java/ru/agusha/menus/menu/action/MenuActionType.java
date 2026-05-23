package ru.agusha.menus.menu.action;

public enum MenuActionType {
    CONSOLE,
    PLAYER,
    MESSAGE,
    CLOSE,
    SOUND,
    OPEN,
    BOOK;

    public static MenuActionType from(String value) {
        try {
            return MenuActionType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return PLAYER;
        }
    }
}
