package ru.agusha.menus.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.agusha.menus.LiteMenus;
import ru.agusha.menus.config.Messages;
import ru.agusha.menus.config.PluginSettings;
import ru.agusha.menus.menu.MenuDefinition;
import ru.agusha.menus.menu.MenuLoadReport;
import ru.agusha.menus.menu.MenuManager;
import ru.agusha.menus.menu.MenuSessionManager;
import ru.agusha.menus.placeholder.PlaceholderService;
import ru.agusha.menus.util.Text;

public final class LiteMenusCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final Messages messages;
    private final PlaceholderService placeholders;
    private final MenuManager menuManager;
    private final MenuSessionManager sessionManager;
    private final MenuCommandManager menuCommandManager;

    public LiteMenusCommand(
            JavaPlugin plugin,
            PluginSettings settings,
            Messages messages,
            PlaceholderService placeholders,
            MenuManager menuManager,
            MenuSessionManager sessionManager,
            MenuCommandManager menuCommandManager
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.placeholders = placeholders;
        this.menuManager = menuManager;
        this.sessionManager = sessionManager;
        this.menuCommandManager = menuCommandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "open" -> open(sender, label, args);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("litemenus.reload")) {
            sender.sendMessage(messages.get("errors.no-permission", "Нет прав"));
            return;
        }

        if (plugin instanceof LiteMenus liteMenus) {
            liteMenus.reloadAll();
        } else {
            settings.reload();
            messages.reload();
            placeholders.reload();
            menuManager.reload();
            menuCommandManager.reload();
        }

        MenuLoadReport report = menuManager.lastReport();
        sender.sendMessage(messages.format(
                "commands.reload",
                "Загруженно %menus% меню. Неудача: %failed%, ошибок: %errors%, варнов: %warnings%",
                "%menus%", report.loadedMenus(),
                "%failed%", report.failedMenus(),
                "%errors%", report.errorCount(),
                "%warnings%", report.warningCount(),
                "%skipped%", report.skippedItems()
        ));
        plugin.getLogger().info(sender.getName() + " reloaded LiteMenus.");
    }

    private void open(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("litemenus.open")) {
            sender.sendMessage(messages.get("errors.no-permission", "Нет прав"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.format("commands.open-usage", "Используй /%label% open <menu> [player]", "%label%", label));
            return;
        }

        MenuDefinition menu = menuManager.getMenu(args[1]).orElse(null);
        if (menu == null) {
            sender.sendMessage(messages.format("errors.unknown-menu", "Меню не найдено &f%menu%", "%menu%", args[1]));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(messages.format("errors.player-not-found", "Игрок не найден &f%player%", "%player%", args[2]));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(messages.format("commands.open-console-usage", "Использование /%label% open <menu> <player>", "%label%", label));
            return;
        }

        menuManager.open(target, menu, sessionManager);
        if (!target.equals(sender)) {
            sender.sendMessage(messages.format(
                    "commands.opened-for-other",
                    "Открото меню &f%menu% для &f%player%&a.",
                    "%menu%", menu.id(),
                    "%player%", target.getName()
            ));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Text.color("&#3CFDE6ᴋ&#3AFBE7ᴏ&#38F9E8ᴍ&#37F7E8ᴍ&#35F6E9ᴀ&#33F4EAʜ&#31F2EBд&#2FF0EBы&#2EEEEC &#2CECEDʟ&#2AEAEEɪ&#28E9EEᴛ&#26E7EFᴇ&#25E5F0ᴍ&#23E3F1ᴇ&#21E1F1ɴ&#1FDFF2ᴜ&#1DDDF3s&#1CDBF4:"));

        sender.sendMessage(Text.color("&#18D8F5/&#16D6F6" + label + " &#11D0F8ʀ&#0FCEF9ᴇ&#0DCDFAʟ&#0BCBFAᴏ&#0AC9FBᴀ&#08C7FCᴅ &#04C3FD- &#01C0FFп&#00BFFFᴇ&#00BFFFᴘ&#00BFFFᴇ&#00BFFFз&#00BFFFᴀ&#00BFFFг&#00BFFFᴘ&#00BFFFу&#00BFFFз&#00BFFFи&#00BFFFт&#00BFFFь &#00BFFFᴍ&#00BFFFᴇ&#00BFFFʜ&#00BFFFю &#00BFFFи &#00BFFFᴋ&#00BFFFᴏ&#00BFFFʜ&#00BFFFȹ&#00BFFFи&#00BFFFг&#00BFFFи"));
        sender.sendMessage(Text.color("&#00BFFF/&#00BFFF" + label + " &#00BFFFᴏ&#00BFFFᴘ&#00BFFFᴇ&#00BFFFɴ &#00BFFF<&#00BFFFᴍ&#00BFFFᴇ&#00BFFFɴ&#00BFFFᴜ&#00BFFF> &#00BFFF[&#00BFFFᴘ&#00BFFFʟ&#00BFFFᴀ&#00BFFFʏ&#00BFFFᴇ&#00BFFFʀ&#00BFFF] &#00BFFF- &#00BFFFᴏ&#00BFFFᴛ&#00BFFFᴋ&#00BFFFᴘ&#00BFFFы&#00BFFFᴛ&#00BFFFь &#00BFFFᴍ&#00BFFFᴇ&#00BFFFʜ&#00BFFFю"));

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return filter(menuManager.getMenus().stream().map(MenuDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                matches.add(value);
            }
        }
        return matches;
    }
}
