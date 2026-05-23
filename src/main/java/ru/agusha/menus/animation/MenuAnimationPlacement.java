package ru.agusha.menus.animation;

import java.util.List;

public record MenuAnimationPlacement(String itemId, List<Integer> slots, boolean remove) {

    public static MenuAnimationPlacement set(String itemId, List<Integer> slots) {
        return new MenuAnimationPlacement(itemId, slots, false);
    }

    public static MenuAnimationPlacement remove(List<Integer> slots) {
        return new MenuAnimationPlacement("", slots, true);
    }
}
