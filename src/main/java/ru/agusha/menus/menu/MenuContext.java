package ru.agusha.menus.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuContext {

    private final Player player;
    private final MenuDefinition menu;
    private final MenuItemDefinition item;
    private final MenuClickType clickType;
    private final InventoryClickEvent clickEvent;

    public MenuContext(Player player, MenuDefinition menu, MenuItemDefinition item) {
        this(player, menu, item, MenuClickType.ANY, null);
    }

    public MenuContext(
            Player player,
            MenuDefinition menu,
            MenuItemDefinition item,
            MenuClickType clickType,
            InventoryClickEvent clickEvent
    ) {
        this.player = player;
        this.menu = menu;
        this.item = item;
        this.clickType = clickType;
        this.clickEvent = clickEvent;
    }

    public Player player() {
        return player;
    }

    public MenuDefinition menu() {
        return menu;
    }

    public MenuItemDefinition item() {
        return item;
    }

    public MenuClickType clickType() {
        return clickType;
    }

    public InventoryClickEvent clickEvent() {
        return clickEvent;
    }
}
