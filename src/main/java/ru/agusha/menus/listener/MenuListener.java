package ru.agusha.menus.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.agusha.menus.config.Messages;
import ru.agusha.menus.menu.MenuClickType;
import ru.agusha.menus.menu.MenuContext;
import ru.agusha.menus.menu.MenuDefinition;
import ru.agusha.menus.menu.MenuItemDefinition;
import ru.agusha.menus.menu.MenuSessionManager;
import ru.agusha.menus.placeholder.PlaceholderService;
import ru.agusha.menus.util.Text;

public final class MenuListener implements Listener {

    private final MenuSessionManager sessions;
    private final ru.agusha.menus.menu.MenuManager menuManager;
    private final PlaceholderService placeholders;
    private final Messages messages;

    public MenuListener(MenuSessionManager sessions, ru.agusha.menus.menu.MenuManager menuManager, PlaceholderService placeholders, Messages messages) {
        this.sessions = sessions;
        this.menuManager = menuManager;
        this.placeholders = placeholders;
        this.messages = messages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        MenuDefinition menu = sessions.get(player).orElse(null);
        if (menu == null) {
            return;
        }

        if (sessions.isAnimating(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            MenuItemDefinition item = menu.itemAt(event.getRawSlot()).orElse(null);
            if (item != null) {
                MenuClickType clickType = MenuClickType.fromBukkit(event.getClick());
                MenuContext context = new MenuContext(player, menu, item, clickType, event);
                if (!canClick(context)) {
                    String permissionMessage = item.permissionMessage().isBlank()
                            ? messages.get("errors.no-item-permission", "Нет прав")
                            : Text.color(placeholders.apply(context, item.permissionMessage()));
                    player.sendMessage(permissionMessage);
                    return;
                }
                item.soundFor(clickType).play(player);
                item.actionsFor(clickType).forEach(action -> action.execute(context, placeholders, menuManager, sessions));
            }
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || sessions.get(player).isEmpty()) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            sessions.get(player).ifPresent(menu -> {
                MenuContext context = new MenuContext(player, menu, null);
                menu.closeActions().forEach(action -> action.execute(context, placeholders, menuManager, sessions));
            });
            sessions.close(player);
        }
    }

    private boolean canClick(MenuContext context) {
        MenuItemDefinition item = context.item();
        if (item.permission() != null && !item.permission().isBlank()
                && !context.player().hasPermission(item.permission())) {
            return false;
        }
        return item.requirementsFor(context.clickType()).stream()
                .allMatch(requirement -> requirement.matches(context, placeholders));
    }
}
