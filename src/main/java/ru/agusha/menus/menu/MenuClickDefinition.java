package ru.agusha.menus.menu;

import java.util.List;
import ru.agusha.menus.menu.action.MenuAction;

public record MenuClickDefinition(List<MenuAction> actions, List<Requirement> requirements, SoundEffect sound) {

    public static MenuClickDefinition empty() {
        return new MenuClickDefinition(List.of(), List.of(), SoundEffect.none());
    }
}
