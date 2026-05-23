package ru.agusha.menus.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.bukkit.ChatColor;

public final class Text {

    private static final Pattern GRADIENT = Pattern.compile(
            "<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>",
            Pattern.DOTALL
    );
    private static final Pattern HEX = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern AMP_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern TAG = Pattern.compile("<(/?)([A-Za-z_]+)>");
    private static final Map<String, ChatColor> COLOR_TAGS = Map.ofEntries(
            Map.entry("black", ChatColor.BLACK),
            Map.entry("dark_blue", ChatColor.DARK_BLUE),
            Map.entry("dark_green", ChatColor.DARK_GREEN),
            Map.entry("dark_aqua", ChatColor.DARK_AQUA),
            Map.entry("dark_red", ChatColor.DARK_RED),
            Map.entry("dark_purple", ChatColor.DARK_PURPLE),
            Map.entry("gold", ChatColor.GOLD),
            Map.entry("gray", ChatColor.GRAY),
            Map.entry("grey", ChatColor.GRAY),
            Map.entry("dark_gray", ChatColor.DARK_GRAY),
            Map.entry("dark_grey", ChatColor.DARK_GRAY),
            Map.entry("blue", ChatColor.BLUE),
            Map.entry("green", ChatColor.GREEN),
            Map.entry("aqua", ChatColor.AQUA),
            Map.entry("red", ChatColor.RED),
            Map.entry("light_purple", ChatColor.LIGHT_PURPLE),
            Map.entry("pink", ChatColor.LIGHT_PURPLE),
            Map.entry("yellow", ChatColor.YELLOW),
            Map.entry("white", ChatColor.WHITE)
    );
    private static final Map<String, ChatColor> DECORATION_TAGS = Map.ofEntries(
            Map.entry("b", ChatColor.BOLD),
            Map.entry("bold", ChatColor.BOLD),
            Map.entry("strong", ChatColor.BOLD),
            Map.entry("i", ChatColor.ITALIC),
            Map.entry("italic", ChatColor.ITALIC),
            Map.entry("em", ChatColor.ITALIC),
            Map.entry("u", ChatColor.UNDERLINE),
            Map.entry("underlined", ChatColor.UNDERLINE),
            Map.entry("underline", ChatColor.UNDERLINE),
            Map.entry("st", ChatColor.STRIKETHROUGH),
            Map.entry("strikethrough", ChatColor.STRIKETHROUGH),
            Map.entry("strike", ChatColor.STRIKETHROUGH),
            Map.entry("obf", ChatColor.MAGIC),
            Map.entry("obfuscated", ChatColor.MAGIC),
            Map.entry("magic", ChatColor.MAGIC)
    );
    private static final Map<String, String> DECORATION_KEYS = Map.ofEntries(
            Map.entry("b", "bold"),
            Map.entry("bold", "bold"),
            Map.entry("strong", "bold"),
            Map.entry("i", "italic"),
            Map.entry("italic", "italic"),
            Map.entry("em", "italic"),
            Map.entry("u", "underline"),
            Map.entry("underlined", "underline"),
            Map.entry("underline", "underline"),
            Map.entry("st", "strikethrough"),
            Map.entry("strikethrough", "strikethrough"),
            Map.entry("strike", "strikethrough"),
            Map.entry("obf", "magic"),
            Map.entry("obfuscated", "magic"),
            Map.entry("magic", "magic")
    );
    private static final Map<String, ChatColor> DECORATION_BY_KEY = Map.ofEntries(
            Map.entry("bold", ChatColor.BOLD),
            Map.entry("italic", ChatColor.ITALIC),
            Map.entry("underline", ChatColor.UNDERLINE),
            Map.entry("strikethrough", ChatColor.STRIKETHROUGH),
            Map.entry("magic", ChatColor.MAGIC)
    );
    private static final Map<String, ChatColor> RESET_TAGS = Map.ofEntries(
            Map.entry("reset", ChatColor.RESET),
            Map.entry("r", ChatColor.RESET)
    );

    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', miniToLegacy(text == null ? "" : text));
    }

    public static List<String> color(List<String> lines) {
        return lines.stream()
                .map(Text::color)
                .toList();
    }

    private static String miniToLegacy(String input) {
        String withGradients = applyGradients(input);
        return applyTags(withGradients).replace("</gradient>", "");
    }

    private static String applyTags(String input) {
        StringBuilder output = new StringBuilder();
        TextState state = new TextState();
        for (int index = 0; index < input.length(); index++) {
            if (startsWithIgnoreCase(input, index, "<br>")) {
                output.append('\n');
                index += 3;
                continue;
            }
            if (startsWithIgnoreCase(input, index, "<newline>")) {
                output.append('\n');
                index += 8;
                continue;
            }
            HexToken hexToken = readHexToken(input, index);
            if (hexToken != null) {
                state.color = hex(hexToken.value());
                output.append(state.fullStyle());
                index += hexToken.length() - 1;
                continue;
            }
            TagToken tagToken = readTagToken(input, index);
            if (tagToken != null) {
                String replacement = state.apply(tagToken.name(), tagToken.closing());
                if (replacement != null) {
                    output.append(replacement);
                    index += tagToken.length() - 1;
                    continue;
                }
            }
            output.append(input.charAt(index));
        }
        return output.toString();
    }

    private static boolean startsWithIgnoreCase(String input, int index, String token) {
        return input.regionMatches(true, index, token, 0, token.length());
    }

    private static HexToken readHexToken(String input, int index) {
        if (index + 9 <= input.length() && input.charAt(index) == '<' && input.charAt(index + 1) == '#'
                && input.charAt(index + 8) == '>') {
            String value = input.substring(index + 2, index + 8);
            if (value.matches("[A-Fa-f0-9]{6}")) {
                return new HexToken(value, 9);
            }
        }
        if (index + 8 <= input.length() && input.charAt(index) == '&' && input.charAt(index + 1) == '#') {
            String value = input.substring(index + 2, index + 8);
            if (value.matches("[A-Fa-f0-9]{6}")) {
                return new HexToken(value, 8);
            }
        }
        return null;
    }

    private static TagToken readTagToken(String input, int index) {
        Matcher matcher = TAG.matcher(input);
        matcher.region(index, input.length());
        if (!matcher.lookingAt()) {
            return null;
        }
        return new TagToken(
                matcher.group(2).toLowerCase(Locale.ROOT),
                !matcher.group(1).isEmpty(),
                matcher.end() - matcher.start()
        );
    }

    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = gradient(matcher.group(1), matcher.group(2), matcher.group(3));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String gradient(String startHex, String endHex, String text) {
        if (text.isEmpty()) {
            return "";
        }

        int start = Integer.parseInt(startHex, 16);
        int end = Integer.parseInt(endHex, 16);
        int startR = (start >> 16) & 0xFF;
        int startG = (start >> 8) & 0xFF;
        int startB = start & 0xFF;
        int endR = (end >> 16) & 0xFF;
        int endG = (end >> 8) & 0xFF;
        int endB = end & 0xFF;
        int length = Math.max(1, text.length() - 1);

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            double ratio = (double) index / length;
            int red = (int) Math.round(startR + (endR - startR) * ratio);
            int green = (int) Math.round(startG + (endG - startG) * ratio);
            int blue = (int) Math.round(startB + (endB - startB) * ratio);
            builder.append(hex(String.format("%02X%02X%02X", red, green, blue))).append(text.charAt(index));
        }
        return builder.toString();
    }

    private static String hex(String value) {
        return net.md_5.bungee.api.ChatColor.of("#" + value).toString();
    }

    private record HexToken(String value, int length) {
    }

    private record TagToken(String name, boolean closing, int length) {
    }

    private static final class TextState {
        private String color = "";
        private final java.util.Set<String> decorations = new java.util.LinkedHashSet<>();

        private String apply(String name, boolean closing) {
            if (RESET_TAGS.containsKey(name)) {
                color = "";
                decorations.clear();
                return ChatColor.RESET.toString();
            }

            ChatColor colorTag = COLOR_TAGS.get(name);
            if (colorTag != null) {
                if (closing) {
                    color = "";
                    return fullStyle();
                }
                color = colorTag.toString();
                return fullStyle();
            }

            String decorationKey = DECORATION_KEYS.get(name);
            if (decorationKey != null) {
                if (closing) {
                    decorations.remove(decorationKey);
                    return fullStyle();
                }
                decorations.add(decorationKey);
                return DECORATION_BY_KEY.get(decorationKey).toString();
            }
            return null;
        }

        private String fullStyle() {
            StringBuilder builder = new StringBuilder(ChatColor.RESET.toString());
            if (!color.isEmpty()) {
                builder.append(color);
            }
            for (String decoration : decorations) {
                builder.append(DECORATION_BY_KEY.get(decoration));
            }
            return builder.toString();
        }
    }
}
