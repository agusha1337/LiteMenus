package ru.agusha.menus.menu;

import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public record SoundEffect(Sound sound, String key, float volume, float pitch) {

    public static SoundEffect none() {
        return new SoundEffect(null, "", 1.0F, 1.0F);
    }

    public static SoundEffect parseStrict(String raw, String path) {
        if (raw == null || raw.isBlank()) {
            return none();
        }

        String[] parts = raw.trim().split("\\s+");
        float volume = parseFloat(parts, 1, 1.0F);
        float pitch = parseFloat(parts, 2, 1.0F);
        String soundName = parts[0].toUpperCase(Locale.ROOT);
        try {
            Sound sound = Sound.valueOf(soundName);
            return new SoundEffect(sound, minecraftKey(soundName), volume, pitch);
        } catch (IllegalArgumentException exception) {
            String key = rawKey(parts[0]);
            if (!key.isBlank()) {
                return new SoundEffect(null, key, volume, pitch);
            }
            throw new MenuValidationException(path + ": не верный звук '" + raw + "'");
        }
    }

    public static SoundEffect parse(String raw, String path, Consumer<String> warning) {
        if (raw == null || raw.isBlank()) {
            return none();
        }

        String[] parts = raw.trim().split("\\s+");
        float volume = parseFloat(parts, 1, 1.0F);
        float pitch = parseFloat(parts, 2, 1.0F);
        String soundName = parts[0].toUpperCase(Locale.ROOT);
        try {
            Sound sound = Sound.valueOf(soundName);
            return new SoundEffect(sound, minecraftKey(soundName), volume, pitch);
        } catch (IllegalArgumentException exception) {
            String key = rawKey(parts[0]);
            if (key.isBlank()) {
                warning.accept(path + ": не верный звук '" + raw + "'.");
                return none();
            }
            return new SoundEffect(null, key, volume, pitch);
        }
    }

    public boolean enabled() {
        return sound != null || !key.isBlank();
    }

    public void play(Player player) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
            return;
        }
        if (!key.isBlank()) {
            player.playSound(player.getLocation(), key, volume, pitch);
        }
    }

    private static float parseFloat(String[] parts, int index, float fallback) {
        if (parts.length <= index) {
            return fallback;
        }
        try {
            return Float.parseFloat(parts[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String rawKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return normalized;
        }
        if (normalized.contains(".")) {
            return "minecraft:" + normalized;
        }
        return minecraftKey(value.toUpperCase(Locale.ROOT));
    }

    private static String minecraftKey(String enumName) {
        return "minecraft:" + enumName.toLowerCase(Locale.ROOT).replace('_', '.');
    }
}
