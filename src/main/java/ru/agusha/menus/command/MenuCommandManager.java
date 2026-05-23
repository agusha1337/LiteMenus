package ru.agusha.menus.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import ru.agusha.menus.config.Messages;
import ru.agusha.menus.config.PluginSettings;
import ru.agusha.menus.menu.MenuDefinition;
import ru.agusha.menus.menu.MenuManager;
import ru.agusha.menus.menu.MenuSessionManager;

public final class MenuCommandManager {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MenuManager menuManager;
    private final MenuSessionManager sessions;
    private final Messages messages;
    private final Map<String, Command> registered = new HashMap<>();

    public MenuCommandManager(
            JavaPlugin plugin,
            PluginSettings settings,
            MenuManager menuManager,
            MenuSessionManager sessions,
            Messages messages
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.menuManager = menuManager;
        this.sessions = sessions;
        this.messages = messages;
    }

    public void reload() {
        unregisterAll();
        if (!settings.autoRegisterMenuCommands()) {
            return;
        }

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            plugin.getLogger().warning("Не удалось получить доступ к Bukkit CommandMap. Команды меню не были зарегистрированы.");
            return;
        }

        Map<String, List<MenuOpenRoute>> routesByRoot = new LinkedHashMap<>();
        Set<String> usedRoutes = new HashSet<>();
        for (MenuDefinition menu : menuManager.getMenus()) {
            for (String commandName : menu.commandNames()) {
                try {
                    Optional<MenuOpenRoute> route = parseRoute(commandName, menu);
                    if (route.isEmpty()) {
                        continue;
                    }
                    String key = route.get().rootCommand() + " " + String.join(" ", route.get().arguments());
                    if (!usedRoutes.add(key)) {
                        plugin.getLogger().warning("Команда меню '" + route.get().usage() + "' уже используется другим меню LiteMenus. Меню '" + menu.id() + "' пропущено.");
                        continue;
                    }
                    routesByRoot.computeIfAbsent(route.get().rootCommand(), ignored -> new ArrayList<>()).add(route.get());
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("Не удалось зарегистрировать команду меню '/" + commandName + "': " + exception.getMessage());
                }
            }
        }

        for (Map.Entry<String, List<MenuOpenRoute>> entry : routesByRoot.entrySet()) {
            register(commandMap, entry.getKey(), entry.getValue());
        }
    }

    public void unregisterAll() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null || registered.isEmpty()) {
            registered.clear();
            return;
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        for (Map.Entry<String, Command> entry : registered.entrySet()) {
            try {
                entry.getValue().unregister(commandMap);
                knownCommands.remove(entry.getKey());
                knownCommands.remove(plugin.getName().toLowerCase(Locale.ROOT) + ":" + entry.getKey());
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Не удалось снять регистрацию команды меню '/" + entry.getKey() + "': " + exception.getMessage());
            }
        }
        registered.clear();
    }

    private void register(CommandMap commandMap, String name, List<MenuOpenRoute> routes) {
        if (registered.containsKey(name)) {
            plugin.getLogger().warning("Команда меню '/" + name + "' уже используется другим меню LiteMenus. Пропускаю.");
            return;
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        Command existing = knownCommands.get(name);
        if (existing != null) {
            plugin.getLogger().warning("Команда меню '/" + name + "' уже зарегистрирована другим плагином. Пропускаю.");
            return;
        }

        MenuOpenCommand command = new MenuOpenCommand(name, routes, menuManager, sessions, messages);
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        registered.put(name, command);
        plugin.getLogger().info("Зарегистрирована команда меню /" + name + " с количеством маршрутов: " + routes.size() + ".");
    }

    private Optional<MenuOpenRoute> parseRoute(String rawName, MenuDefinition menu) {
        String normalized = rawName.toLowerCase(Locale.ROOT)
                .replaceFirst("^/", "")
                .replace('/', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String[] parts = normalized.split("\\s+");
        String root = parts[0];
        if (!root.matches("[\\p{L}0-9_\\-]+")) {
            plugin.getLogger().warning("В меню '" + menu.id() + "' указана некорректная команда открытия: '" + rawName + "'.");
            return Optional.empty();
        }

        List<String> arguments = new ArrayList<>();
        for (int index = 1; index < parts.length; index++) {
            String argument = parts[index].trim();
            if (!argument.isBlank()) {
                arguments.add(argument);
            }
        }
        return Optional.of(new MenuOpenRoute(rawName, root, arguments, menu));
    }

    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        if (!(commandMap instanceof SimpleCommandMap simpleCommandMap)) {
            return new HashMap<>();
        }

        try {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            return (Map<String, Command>) field.get(simpleCommandMap);
        } catch (ReflectiveOperationException exception) {
            return new HashMap<>();
        }
    }
}
