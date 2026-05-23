package ru.agusha.menus.menu;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import ru.agusha.menus.animation.MenuAnimationService;
import ru.agusha.menus.placeholder.PlaceholderService;
import ru.agusha.menus.util.Text;

public final class MenuManager {

    private final JavaPlugin plugin;
    private final MenuLoader loader;
    private final PlaceholderService placeholders;
    private final MenuAnimationService animations;
    private Map<String, MenuDefinition> menus = new LinkedHashMap<>();
    private MenuLoadReport lastReport = new MenuLoadReport();

    public MenuManager(JavaPlugin plugin, MenuLoader loader, PlaceholderService placeholders, MenuAnimationService animations) {
        this.plugin = plugin;
        this.loader = loader;
        this.placeholders = placeholders;
        this.animations = animations;
    }

    public void reload() {
        MenuLoadResult result = loader.loadMenus();
        this.menus = result.menus();
        this.lastReport = result.report();
    }

    public Optional<MenuDefinition> getMenu(String id) {
        return Optional.ofNullable(menus.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<MenuDefinition> getMenuByCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT).replaceFirst("^/", "").trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return menus.values().stream()
                .filter(menu -> menu.commandNames().contains(normalized))
                .findFirst();
    }

    public Collection<MenuDefinition> getMenus() {
        return menus.values();
    }

    public MenuLoadReport lastReport() {
        return lastReport;
    }

    public void open(Player player, MenuDefinition menu, MenuSessionManager sessions) {
        if (menu.type() == MenuType.BOOK) {
            openBook(player, menu, sessions);
            return;
        }

        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(menu.id()),
                menu.size(),
                Text.color(placeholders.apply(player, menu.title()))
        );
        player.openInventory(inventory);
        sessions.open(player, menu);
        animations.play(player, menu, inventory, sessions, item -> {
            if (canView(player, menu, item)) {
                for (int slot : item.slots()) {
                    inventory.setItem(slot, item.createItemStack(placeholders, player));
                }
            }
        });
        MenuContext context = new MenuContext(player, menu, null);
        menu.openActions().forEach(action -> action.execute(context, placeholders, this, sessions));
    }

    public void openNextTick(Player player, MenuDefinition menu, MenuSessionManager sessions) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                open(player, menu, sessions);
            }
        });
    }

    private void openBook(Player player, MenuDefinition menu, MenuSessionManager sessions) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setTitle(Text.color(placeholders.apply(player, menu.title())));
        meta.setAuthor(Text.color(placeholders.apply(player, menu.author())));
        for (BookPageDefinition page : menu.pages()) {
            StringBuilder builder = new StringBuilder();
            if (page.title() != null && !page.title().isBlank()) {
                builder.append(page.title()).append("\n\n");
            }
            builder.append(String.join("\n", page.lines()));
            meta.addPage(Text.color(placeholders.apply(player, builder.toString())));
        }
        book.setItemMeta(meta);
        player.openBook(book);
        MenuContext context = new MenuContext(player, menu, null);
        menu.openActions().forEach(action -> action.execute(context, placeholders, this, sessions));
    }

    public void refresh(Player player, MenuDefinition menu) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory.getHolder() instanceof MenuHolder holder)
                || !holder.getMenuId().equals(menu.id())) {
            return;
        }

        inventory.clear();
        for (MenuItemDefinition item : menu.slottedItems()) {
            if (canView(player, menu, item)) {
                for (int slot : item.slots()) {
                    inventory.setItem(slot, item.createItemStack(placeholders, player));
                }
            }
        }
    }

    private boolean canView(Player player, MenuDefinition menu, MenuItemDefinition item) {
        MenuContext context = new MenuContext(player, menu, item);
        return item.viewRequirements().stream()
                .allMatch(requirement -> requirement.matches(context, placeholders));
    }
}
