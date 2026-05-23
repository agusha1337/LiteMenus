package ru.agusha.menus.config;

public final class DefaultResources {

    private DefaultResources() {
    }

    public static String content(String path) {
        return switch (path) {
            case "config.yml" -> """
                    debug: false
                    default-menu-size: 54
                    placeholder-api: true
                    auto-register-menu-commands: true
                    """;
            case "messages.yml" -> """
                    prefix: "&#3CFDE6L&#2BECEDi&#1BDBF4t&#0AC9FBe&#00BFFFM&#00BFFFe&#00BFFFn&#00BFFFu&#00BFFFs &8• "

                    commands:
                      reload: "%prefix%&fМеню перечитаны: &#3CFDE6%menus% &7| &fошибок меню: &#FF5C5C%failed% &7| &fстрок ошибок: &#FFB86C%errors%&7."
                      open-usage: "%prefix%&7Используй так: &#3CFDE6/%label% open <menu> [player]"
                      open-console-usage: "%prefix%&7Из консоли нужно указать игрока: &#3CFDE6/%label% open <menu> <player>"
                      opened-for-other: "%prefix%&fОткрыл меню &#3CFDE6%menu% &fдля &#3CFDE6%player%&f."

                    errors:
                      no-permission: "%prefix%&#FF5C5CНет доступа. &7Похоже, тебе не выдали нужное право."
                      no-item-permission: "%prefix%&#FF5C5CЭтот пункт меню тебе недоступен."
                      player-only: "%prefix%&7Эту команду можно выполнить только от лица игрока."
                      unknown-menu: "%prefix%&#FF5C5CМеню &#FFFFFF%menu% &#FF5C5Cне найдено. &7Проверь имя файла в папке menus."
                      player-not-found: "%prefix%&#FF5C5CИгрок &#FFFFFF%player% &#FF5C5Cне найден онлайн."
                      unknown-menu-command: "%prefix%&#FF5C5CНеизвестная команда меню. &7Доступно: &#FFFFFF%usage%"
                    """;
            default -> "";
        };
    }
}
