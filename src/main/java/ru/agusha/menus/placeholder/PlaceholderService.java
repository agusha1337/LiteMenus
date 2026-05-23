package ru.agusha.menus.placeholder;

import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.agusha.menus.config.PluginSettings;
import ru.agusha.menus.menu.MenuContext;

public final class PlaceholderService {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private Method placeholderMethod;

    public PlaceholderService(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void reload() {
        this.placeholderMethod = null;
        if (!settings.placeholderApi()) {
            return;
        }

        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.placeholderMethod = apiClass.getMethod("setPlaceholders", Player.class, String.class);
            plugin.getLogger().info("PlaceholderAPI включен");
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
        }
    }

    public String apply(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (placeholderMethod == null || player == null) {
            return text;
        }
        try {
            return String.valueOf(placeholderMethod.invoke(null, player, text));
        } catch (ReflectiveOperationException exception) {
            return text;
        }
    }

    public String apply(MenuContext context, String text) {
        String parsed = text == null ? "" : text;
        if (context != null) {
            parsed = parsed.replace("%player%", context.player().getName())
                    .replace("%menu%", context.menu().id())
                    .replace("%item%", context.item() == null ? "" : context.item().id());
            return apply(context.player(), parsed);
        }
        return parsed;
    }

    public List<String> apply(Player player, List<String> lines) {
        return lines.stream()
                .map(line -> apply(player, line))
                .toList();
    }
}
