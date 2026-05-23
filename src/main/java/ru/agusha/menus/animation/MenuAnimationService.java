package ru.agusha.menus.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.agusha.menus.menu.MenuDefinition;
import ru.agusha.menus.menu.MenuItemDefinition;
import ru.agusha.menus.menu.MenuSessionManager;
import ru.agusha.menus.placeholder.PlaceholderService;

public final class MenuAnimationService {

    private final JavaPlugin plugin;
    private final PlaceholderService placeholders;

    public MenuAnimationService(JavaPlugin plugin, PlaceholderService placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    public void play(
            Player player,
            MenuDefinition menu,
            Inventory inventory,
            MenuSessionManager sessions,
            java.util.function.Consumer<MenuItemDefinition> placeItem
    ) {
        MenuAnimationDefinition animation = menu.openAnimation();
        if (!animation.enabled()) {
            menu.slottedItems().forEach(placeItem);
            sessions.finishAnimation(player);
            return;
        }

        if (animation.type() == MenuAnimationType.SCRIPT) {
            playScript(player, menu, inventory, sessions, animation);
            return;
        }

        List<Integer> slots = slots(menu.size(), animation.type());
        ItemStack filler = new ItemStack(animation.fillerMaterial() == null
                ? Material.BLACK_STAINED_GLASS_PANE
                : animation.fillerMaterial());
        sessions.startAnimation(player);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    sessions.cancelAnimation(player);
                    return;
                }

                if (index < slots.size()) {
                    inventory.setItem(slots.get(index), filler);
                    playSound(player, animation);
                    index++;
                    return;
                }

                inventory.clear();
                menu.slottedItems().forEach(placeItem);
                sessions.finishAnimation(player);
            }
        }, 0L, Math.max(1L, animation.interval()));
        sessions.setAnimationTask(player, task);
    }

    private void playScript(Player player, MenuDefinition menu, Inventory inventory, MenuSessionManager sessions, MenuAnimationDefinition animation) {
        List<MenuAnimationFrame> frames = animation.frames();
        if (frames.isEmpty()) {
            menu.slottedItems().forEach(item -> {
                ItemStack stack = item.createItemStack(placeholders, player);
                for (int slot : item.slots()) {
                    inventory.setItem(slot, stack.clone());
                }
            });
            sessions.finishAnimation(player);
            return;
        }

        sessions.startAnimation(player);
        int lastTick = frames.stream().mapToInt(MenuAnimationFrame::tick).max().orElse(0);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int tick;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    sessions.cancelAnimation(player);
                    return;
                }

                for (MenuAnimationFrame frame : frames) {
                    if (frame.tick() == tick) {
                        for (MenuAnimationPlacement placement : frame.placements()) {
                            if (placement.remove()) {
                                for (int slot : placement.slots()) {
                                    if (slot >= 0 && slot < inventory.getSize()) {
                                        inventory.clear(slot);
                                    }
                                }
                                continue;
                            }
                            menu.item(placement.itemId()).ifPresent(item -> {
                                ItemStack stack = item.createItemStack(placeholders, player);
                                for (int slot : placement.slots()) {
                                    if (slot >= 0 && slot < inventory.getSize()) {
                                        inventory.setItem(slot, stack.clone());
                                    }
                                }
                            });
                        }
                        if (frame.sound().enabled()) {
                            frame.sound().play(player);
                        } else {
                            playSound(player, animation);
                        }
                    }
                }

                if (tick > lastTick) {
                    sessions.finishAnimation(player);
                    return;
                }
                tick++;
            }
        }, 0L, Math.max(1L, animation.interval()));
        sessions.setAnimationTask(player, task);
    }

    private void playSound(Player player, MenuAnimationDefinition animation) {
        if (animation.sound() != null) {
            player.playSound(player.getLocation(), animation.sound(), animation.volume(), animation.pitch());
        }
    }

    private List<Integer> slots(int size, MenuAnimationType type) {
        return switch (type) {
            case FILL -> fill(size);
            case BORDER -> border(size);
            case ROWS -> fill(size);
            case CENTER -> center(size);
            case NONE, SCRIPT -> List.of();
        };
    }

    private List<Integer> fill(int size) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> border(int size) {
        int rows = size / 9;
        List<Integer> border = new ArrayList<>();
        List<Integer> inner = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                border.add(slot);
            } else {
                inner.add(slot);
            }
        }
        border.addAll(inner);
        return border;
    }

    private List<Integer> center(int size) {
        int rows = size / 9;
        double centerRow = (rows - 1) / 2.0D;
        double centerColumn = 4.0D;
        List<Integer> slots = fill(size);
        slots.sort(Comparator.comparingDouble(slot -> {
            int row = slot / 9;
            int column = slot % 9;
            return Math.abs(row - centerRow) + Math.abs(column - centerColumn);
        }));
        return slots;
    }
}
