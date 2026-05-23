package ru.agusha.menus.menu;

import org.bukkit.potion.PotionEffectType;

public record PotionEffectDefinition(
        PotionEffectType type,
        int duration,
        int amplifier,
        boolean ambient,
        boolean particles,
        boolean icon
) {
}
