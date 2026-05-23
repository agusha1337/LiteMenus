package ru.agusha.menus.menu.action;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import ru.agusha.menus.menu.MenuContext;
import ru.agusha.menus.menu.MenuManager;
import ru.agusha.menus.menu.MenuSessionManager;
import ru.agusha.menus.placeholder.PlaceholderService;
import ru.agusha.menus.util.Text;

public final class MenuAction {

    private final MenuActionType type;
    private final String payload;

    private MenuAction(MenuActionType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public static MenuAction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new MenuAction(MenuActionType.MESSAGE, "");
        }

        String trimmed = raw.trim();
        if (!trimmed.startsWith("[") || !trimmed.contains("]")) {
            return new MenuAction(MenuActionType.PLAYER, trimmed);
        }

        int end = trimmed.indexOf(']');
        String prefix = trimmed.substring(1, end).toUpperCase(Locale.ROOT);
        String payload = trimmed.substring(end + 1).trim();
        MenuActionType type = MenuActionType.from(prefix);
        return new MenuAction(type, payload);
    }

    public void execute(MenuContext context, PlaceholderService placeholders) {
        execute(context, placeholders, null, null);
    }

    public void execute(MenuContext context, PlaceholderService placeholders, MenuManager menuManager, MenuSessionManager sessions) {
        String parsedPayload = placeholders.apply(context, payload);

        switch (type) {
            case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.color(parsedPayload));
            case PLAYER -> context.player().performCommand(Text.color(parsedPayload));
            case MESSAGE -> context.player().sendMessage(Text.color(parsedPayload));
            case CLOSE -> context.player().closeInventory();
            case SOUND -> playSound(context, parsedPayload);
            case OPEN, BOOK -> openMenu(context, parsedPayload, menuManager, sessions);
        }
    }

    private void openMenu(MenuContext context, String menuId, MenuManager menuManager, MenuSessionManager sessions) {
        if (menuManager == null || sessions == null || menuId.isBlank()) {
            return;
        }
        menuManager.getMenu(menuId)
                .or(() -> menuManager.getMenuByCommand(menuId))
                .ifPresent(menu -> menuManager.openNextTick(context.player(), menu, sessions));
    }

    private void playSound(MenuContext context, String rawSound) {
        if (rawSound.isBlank()) {
            return;
        }

        String[] parts = rawSound.split("\\s+");
        try {
            Sound sound = Sound.valueOf(parts[0].toUpperCase(Locale.ROOT));
            float volume = parts.length >= 2 ? Float.parseFloat(parts[1]) : 1.0F;
            float pitch = parts.length >= 3 ? Float.parseFloat(parts[2]) : 1.0F;
            context.player().playSound(context.player().getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            context.player().sendMessage(Text.color("&cUnknown sound: " + parts[0]));
        }
    }
}
