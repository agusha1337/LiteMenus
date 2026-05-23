package ru.agusha.menus.command;

import java.util.List;
import ru.agusha.menus.menu.MenuDefinition;

public record MenuOpenRoute(String rawCommand, String rootCommand, List<String> arguments, MenuDefinition menu) {

    public String usage() {
        if (arguments.isEmpty()) {
            return "/" + rootCommand;
        }
        return "/" + rootCommand + " " + String.join(" ", arguments);
    }
}
