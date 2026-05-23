package ru.agusha.menus.animation;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;

public record MenuAnimationDefinition(
        MenuAnimationType type,
        int interval,
        Material fillerMaterial,
        Sound sound,
        float volume,
        float pitch,
        List<MenuAnimationFrame> frames
) {

    public static MenuAnimationDefinition none() {
        return new MenuAnimationDefinition(MenuAnimationType.NONE, 1, Material.BLACK_STAINED_GLASS_PANE, null, 1.0F, 1.0F, List.of());
    }

    public boolean enabled() {
        return type != MenuAnimationType.NONE;
    }
}
