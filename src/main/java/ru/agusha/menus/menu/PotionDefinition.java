package ru.agusha.menus.menu;

import java.util.List;
import org.bukkit.Color;
import org.bukkit.potion.PotionType;

public record PotionDefinition(Color color, PotionType baseType, List<PotionEffectDefinition> effects) {

    public static PotionDefinition empty() {
        return new PotionDefinition(null, null, List.of());
    }

    public boolean hasData() {
        return color != null || baseType != null || !effects.isEmpty();
    }
}
