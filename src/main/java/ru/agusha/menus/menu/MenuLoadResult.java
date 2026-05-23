package ru.agusha.menus.menu;

import java.util.Map;

public record MenuLoadResult(Map<String, MenuDefinition> menus, MenuLoadReport report) {
}
