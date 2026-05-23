package ru.agusha.menus.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginSettings {

    private final JavaPlugin plugin;
    private boolean debug;
    private int defaultMenuSize;
    private boolean placeholderApi;
    private boolean autoRegisterMenuCommands;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        this.debug = config.getBoolean("debug", false);
        this.defaultMenuSize = normalizeSize(config.getInt("default-menu-size", 54));
        this.placeholderApi = config.getBoolean("placeholder-api", true);
        this.autoRegisterMenuCommands = config.getBoolean("auto-register-menu-commands", true);
    }

    public boolean debug() {
        return debug;
    }

    public int defaultMenuSize() {
        return defaultMenuSize;
    }

    public boolean placeholderApi() {
        return placeholderApi;
    }

    public boolean autoRegisterMenuCommands() {
        return autoRegisterMenuCommands;
    }

    private int normalizeSize(int size) {
        if (size >= 9 && size <= 54 && size % 9 == 0) {
            return size;
        }
        return 54;
    }
}
