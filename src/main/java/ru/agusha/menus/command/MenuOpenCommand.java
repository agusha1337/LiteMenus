package ru.agusha.menus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.agusha.menus.config.Messages;
import ru.agusha.menus.menu.MenuManager;
import ru.agusha.menus.menu.MenuSessionManager;

public final class MenuOpenCommand extends Command {

    private final List<MenuOpenRoute> routes;
    private final MenuManager menuManager;
    private final MenuSessionManager sessions;
    private final Messages messages;

    public MenuOpenCommand(
            String name,
            List<MenuOpenRoute> routes,
            MenuManager menuManager,
            MenuSessionManager sessions,
            Messages messages
    ) {
        super(name);
        this.routes = List.copyOf(routes);
        this.menuManager = menuManager;
        this.sessions = sessions;
        this.messages = messages;
        setDescription("Open LiteMenus menu.");
        setUsage(buildUsage());
        setPermission("litemenus.open");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("errors.player-only", "&cOnly players can use this command."));
            return true;
        }
        if (!sender.hasPermission("litemenus.open")) {
            sender.sendMessage(messages.get("errors.no-permission", "&cYou do not have permission."));
            return true;
        }

        MenuOpenRoute route = findRoute(args);
        if (route == null) {
            sender.sendMessage(messages.get(
                    "errors.unknown-menu-command",
                    "&cНеизвестная команда меню. Доступно: &f%usage%"
            ).replace("%usage%", buildUsage()));
            return true;
        }

        menuManager.open(player, route.menu(), sessions);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission("litemenus.open")) {
            return List.of();
        }

        List<String> normalized = normalize(args);
        int index = Math.max(0, args.length - 1);
        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (MenuOpenRoute route : routes) {
            if (route.arguments().size() <= index) {
                continue;
            }
            if (!matchesPrefix(route.arguments(), normalized, index)) {
                continue;
            }
            String suggestion = route.arguments().get(index);
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(current) && !suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        return suggestions;
    }

    private MenuOpenRoute findRoute(String[] args) {
        List<String> normalized = normalize(args);
        for (MenuOpenRoute route : routes) {
            if (route.arguments().equals(normalized)) {
                return route;
            }
        }
        return null;
    }

    private boolean matchesPrefix(List<String> routeArguments, List<String> typedArguments, int currentIndex) {
        for (int i = 0; i < currentIndex; i++) {
            if (i >= routeArguments.size() || i >= typedArguments.size()) {
                return false;
            }
            if (!routeArguments.get(i).equals(typedArguments.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> normalize(String[] args) {
        List<String> normalized = new ArrayList<>();
        for (String arg : args) {
            if (arg != null && !arg.isBlank()) {
                normalized.add(arg.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private String buildUsage() {
        return routes.stream()
                .map(MenuOpenRoute::usage)
                .reduce((left, right) -> left + ", " + right)
                .orElse("/" + getName());
    }
}
