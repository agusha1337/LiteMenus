package ru.agusha.menus.menu;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.agusha.menus.animation.MenuAnimationFrame;
import ru.agusha.menus.animation.MenuAnimationPlacement;
import ru.agusha.menus.animation.MenuAnimationType;
import ru.agusha.menus.animation.MenuAnimationDefinition;
import ru.agusha.menus.menu.action.MenuAction;

public final class MenuDefinition {

    private final String id;
    private final MenuType type;
    private final String title;
    private final String author;
    private final int size;
    private final String openCommand;
    private final List<String> openAliases;
    private final boolean update;
    private final int updateInterval;
    private final MenuAnimationDefinition openAnimation;
    private final List<MenuAction> openActions;
    private final List<MenuAction> closeActions;
    private final List<BookPageDefinition> pages;
    private final Map<String, MenuItemDefinition> items;

    public MenuDefinition(
            String id,
            MenuType type,
            String title,
            String author,
            int size,
            String openCommand,
            List<String> openAliases,
            boolean update,
            int updateInterval,
            MenuAnimationDefinition openAnimation,
            List<MenuAction> openActions,
            List<MenuAction> closeActions,
            List<BookPageDefinition> pages,
            Map<String, MenuItemDefinition> items
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.author = author;
        this.size = size;
        this.openCommand = openCommand;
        this.openAliases = List.copyOf(openAliases);
        this.update = update;
        this.updateInterval = updateInterval;
        this.openAnimation = openAnimation;
        this.openActions = List.copyOf(openActions);
        this.closeActions = List.copyOf(closeActions);
        this.pages = List.copyOf(pages);
        this.items = new LinkedHashMap<>(items);
    }

    public String id() {
        return id;
    }

    public MenuType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public int size() {
        return size;
    }

    public Optional<String> openCommand() {
        return Optional.ofNullable(openCommand);
    }

    public List<String> openAliases() {
        return openAliases;
    }

    public List<String> commandNames() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        openCommand().ifPresent(names::add);
        names.addAll(openAliases);
        return names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(java.util.Locale.ROOT).replaceFirst("^/", ""))
                .distinct()
                .toList();
    }

    public boolean update() {
        return update;
    }

    public int updateInterval() {
        return updateInterval;
    }

    public MenuAnimationDefinition openAnimation() {
        return openAnimation;
    }

    public List<MenuAction> openActions() {
        return openActions;
    }

    public List<MenuAction> closeActions() {
        return closeActions;
    }

    public List<BookPageDefinition> pages() {
        return pages;
    }

    public Collection<MenuItemDefinition> items() {
        return items.values();
    }

    public Collection<MenuItemDefinition> slottedItems() {
        return items.values().stream()
                .filter(item -> !item.slots().isEmpty())
                .toList();
    }

    public Optional<MenuItemDefinition> item(String itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    public Optional<MenuItemDefinition> itemAt(int slot) {
        Optional<MenuItemDefinition> slottedItem = items.values().stream()
                .filter(item -> item.slots().contains(slot))
                .findFirst();
        if (slottedItem.isPresent()) {
            return slottedItem;
        }
        return animatedItemAt(slot);
    }

    private Optional<MenuItemDefinition> animatedItemAt(int slot) {
        if (openAnimation.type() != MenuAnimationType.SCRIPT) {
            return Optional.empty();
        }

        String itemId = null;
        for (MenuAnimationFrame frame : openAnimation.frames()) {
            for (MenuAnimationPlacement placement : frame.placements()) {
                if (placement.remove() && placement.slots().contains(slot)) {
                    itemId = null;
                    continue;
                }
                if (placement.slots().contains(slot)) {
                    itemId = placement.itemId();
                }
            }
        }
        return itemId == null ? Optional.empty() : item(itemId);
    }
}
