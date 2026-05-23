package ru.agusha.menus.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.agusha.menus.util.Text;

public final class Messages {

    private final JavaPlugin plugin;
    private YamlConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveMessagesFallback(file);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path) {
        return Text.color(withPrefix(config.getString(path, "&cMissing message: " + path)));
    }

    public String get(String path, String fallback) {
        return Text.color(withPrefix(config.getString(path, fallback)));
    }

    public String format(String path, String fallback, Object... placeholders) {
        String message = get(path, fallback);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            message = message.replace(String.valueOf(placeholders[index]), String.valueOf(placeholders[index + 1]));
        }
        return message;
    }

    private String withPrefix(String message) {
        return message.replace("%prefix%", config.getString("prefix", ""));
    }

    private void saveMessagesFallback(File file) {
        if (plugin.getResource("messages.yml") != null) {
            plugin.saveResource("messages.yml", false);
            return;
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin folder for messages.yml.");
            return;
        }

        try {
            Files.writeString(file.toPath(), DefaultResources.content("messages.yml"), StandardCharsets.UTF_8);
            plugin.getLogger().warning("messages.yml was missing inside the jar. Created fallback file.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not create messages.yml fallback: " + exception.getMessage());
        }
    }
}
