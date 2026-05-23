package ru.agusha.menus.menu;

import ru.agusha.menus.placeholder.PlaceholderService;

public final class Requirement {

    private final RequirementType type;
    private final String input;
    private final String expected;

    public Requirement(RequirementType type, String input, String expected) {
        this.type = type;
        this.input = input;
        this.expected = expected;
    }

    public boolean matches(MenuContext context, PlaceholderService placeholders) {
        return switch (type) {
            case PERMISSION -> context.player().hasPermission(input);
            case HAS_PLACEHOLDER -> !placeholders.apply(context, input).isBlank();
            case EQUALS_PLACEHOLDER -> placeholders.apply(context, input).equals(placeholders.apply(context, expected));
        };
    }
}
