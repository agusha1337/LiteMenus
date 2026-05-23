package ru.agusha.menus.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import ru.agusha.menus.animation.MenuAnimationDefinition;
import ru.agusha.menus.animation.MenuAnimationFrame;
import ru.agusha.menus.animation.MenuAnimationPlacement;
import ru.agusha.menus.animation.MenuAnimationType;
import ru.agusha.menus.config.PluginSettings;
import ru.agusha.menus.menu.action.MenuAction;

public final class MenuLoader {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private MenuLoadReport report = new MenuLoadReport();
    private List<String> currentErrors = new ArrayList<>();

    public MenuLoader(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public MenuLoadResult loadMenus() {
        this.report = new MenuLoadReport();
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists() && !menusFolder.mkdirs()) {
            warn("menus", "Could not create menus folder: " + menusFolder.getAbsolutePath());
            return new MenuLoadResult(Map.of(), report);
        }

        List<File> files = findMenuFiles(menusFolder);
        if (files.isEmpty()) {
            return new MenuLoadResult(Map.of(), report);
        }

        Map<String, MenuDefinition> menus = new LinkedHashMap<>();
        Map<String, Integer> filenameCounts = countFileNames(files);
        for (File file : files) {
            String menuPath = menuPath(menusFolder, file);
            try {
                this.currentErrors = new ArrayList<>();
                java.util.Optional<MenuDefinition> loaded = loadMenu(menusFolder, file, filenameCounts);
                if (!currentErrors.isEmpty()) {
                    report.failedMenu();
                    error(menuPath, "Menu was not loaded because it has " + currentErrors.size() + " error(s).");
                    continue;
                }
                loaded.ifPresent(menu -> {
                    if (menus.containsKey(menu.id())) {
                        report.failedMenu();
                        error(menuPath, "Duplicate menu id '" + menu.id() + "'. Rename one of the files or move it to another path.");
                    } else {
                        menus.put(menu.id(), menu);
                        report.loadedMenu();
                    }
                });
            } catch (RuntimeException exception) {
                report.failedMenu();
                error(menuPath, "Could not load menu: " + exception.getMessage());
            }
        }
        return new MenuLoadResult(menus, report);
    }

    private List<File> findMenuFiles(File menusFolder) {
        try (java.util.stream.Stream<Path> stream = Files.walk(menusFolder.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    .sorted()
                    .map(Path::toFile)
                    .toList();
        } catch (IOException exception) {
            warn("menus", "Could not scan menus folder recursively: " + exception.getMessage());
            return List.of();
        }
    }

    private java.util.Optional<MenuDefinition> loadMenu(File menusFolder, File file, Map<String, Integer> filenameCounts) {
        String id = menuId(menusFolder, file, filenameCounts);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        MenuType type = MenuType.from(config.getString("type", "CHEST"));
        String title = config.getString("title", "&8" + id);
        String author = config.getString("author", "LiteMenus");
        int size = normalizeSize(config.getInt("size", settings.defaultMenuSize()), id);
        if (type == MenuType.CHEST && config.getConfigurationSection("items") == null) {
            fail("menus/" + id + ".yml -> items", "missing required section");
        }
        List<BookPageDefinition> pages = type == MenuType.BOOK ? loadBookPages(id, config) : List.of();
        String openCommand = readOpenCommand(config, baseMenuId(file));
        List<String> openAliases = config.getStringList("open-aliases").stream()
                .map(alias -> alias.toLowerCase(Locale.ROOT).replaceFirst("^/", "").trim())
                .filter(alias -> !alias.isBlank())
                .distinct()
                .toList();
        boolean update = config.getBoolean("update", false);
        int updateInterval = Math.max(1, config.getInt("update-interval", 20));
        MenuAnimationDefinition openAnimation = config.isList("animation")
                ? loadScriptAnimation(id, config)
                : loadOpenAnimation(id, config.getConfigurationSection("animation.open"));
        List<MenuAction> openActions = parseActions(config.getStringList("open-commands"));
        List<MenuAction> closeActions = parseActions(config.getStringList("close-commands"));
        Map<String, MenuItemDefinition> items = new LinkedHashMap<>();

        ConfigurationSection itemSection = config.getConfigurationSection("items");
        if (type == MenuType.CHEST && itemSection != null) {
            for (String itemId : itemSection.getKeys(false)) {
                ConfigurationSection section = itemSection.getConfigurationSection(itemId);
                if (section == null) {
                    continue;
                }
                MenuItemDefinition item = loadItem(id, itemId, section, size);
                if (item != null) {
                    items.put(item.id(), item);
                } else {
                    report.skippedItem();
                }
            }
        }

        return java.util.Optional.of(new MenuDefinition(
                id,
                type,
                title,
                author,
                size,
                openCommand,
                openAliases,
                update,
                updateInterval,
                openAnimation,
                openActions,
                closeActions,
                pages,
                items
        ));
    }

    private Map<String, Integer> countFileNames(List<File> files) {
        Map<String, Integer> counts = new HashMap<>();
        for (File file : files) {
            String id = baseMenuId(file);
            counts.put(id, counts.getOrDefault(id, 0) + 1);
        }
        return counts;
    }

    private String menuId(File menusFolder, File file, Map<String, Integer> filenameCounts) {
        String baseId = baseMenuId(file);
        if (filenameCounts.getOrDefault(baseId, 0) <= 1) {
            return baseId;
        }
        String relative = menusFolder.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
        return relative.substring(0, relative.length() - 4).toLowerCase(Locale.ROOT);
    }

    private String baseMenuId(File file) {
        String name = file.getName();
        return name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
    }

    private String menuPath(File menusFolder, File file) {
        return "menus/" + menusFolder.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    private List<BookPageDefinition> loadBookPages(String menuId, YamlConfiguration config) {
        List<BookPageDefinition> pages = new ArrayList<>();
        if (!config.isList("pages")) {
            fail("menus/" + menuId + ".yml -> pages", "BOOK menu requires pages list");
            return pages;
        }

        int index = 0;
        for (Map<?, ?> rawPage : config.getMapList("pages")) {
            String title = String.valueOf(rawPage.containsKey("title") ? rawPage.get("title") : "");
            Object rawLines = rawPage.get("lines");
            List<String> lines = new ArrayList<>();
            if (rawLines instanceof List<?> list) {
                for (Object line : list) {
                    lines.add(String.valueOf(line));
                }
            } else {
                fail("menus/" + menuId + ".yml -> pages[" + index + "].lines", "missing lines list");
            }
            pages.add(new BookPageDefinition(title, lines));
            index++;
        }
        if (pages.isEmpty()) {
            fail("menus/" + menuId + ".yml -> pages", "BOOK menu must contain at least one page");
        }
        return pages;
    }

    private MenuItemDefinition loadItem(String menuId, String itemId, ConfigurationSection section, int menuSize) {
        List<Integer> slots = loadSlots(menuId, itemId, section.get("slot"), menuSize);

        String rawMaterial = section.getString("material", "STONE").trim();
        String headTexture = "";
        Material material;
        if (rawMaterial.toLowerCase(Locale.ROOT).startsWith("basehead-")) {
            headTexture = rawMaterial.substring("basehead-".length()).trim();
            material = Material.matchMaterial("PLAYER_HEAD");
            if (headTexture.isBlank()) {
                fail("menus/" + menuId + ".yml -> items." + itemId + ".material", "basehead texture is empty");
            }
        } else {
            material = Material.matchMaterial(rawMaterial.toUpperCase(Locale.ROOT));
        }
        if (material == null || material.isAir()) {
            fail("menus/" + menuId + ".yml -> items." + itemId + ".material", "invalid material '" + rawMaterial + "'");
            material = Material.STONE;
            headTexture = "";
        }

        List<ItemFlag> flags = new ArrayList<>();
        List<String> rawFlags = new ArrayList<>(section.getStringList("flags"));
        rawFlags.addAll(section.getStringList("item_flags"));
        for (String rawFlag : rawFlags) {
            ItemFlag flag = resolveItemFlag(rawFlag);
            if (flag == null) {
                fail("menus/" + menuId + ".yml -> items." + itemId + ".item_flags", "unknown item flag '" + rawFlag + "'");
            } else if (!flags.contains(flag)) {
                flags.add(flag);
            }
        }

        List<MenuAction> fallbackActions = parseActions(section.getStringList("commands"));
        Map<MenuClickType, MenuClickDefinition> clicks = loadClicks(section.getConfigurationSection("clicks"));
        EnchantmentLoadResult enchantmentResult = loadEnchantments(menuId, itemId, section.getConfigurationSection("enchantments"));
        boolean glow = section.getBoolean("glow", false) || enchantmentResult.glintOverride();

        return new MenuItemDefinition(
                itemId.toLowerCase(Locale.ROOT),
                slots,
                material,
                headTexture,
                Math.max(1, section.getInt("amount", 1)),
                Math.max(0, section.getInt("damage", 0)),
                section.getString("name", ""),
                section.getStringList("lore"),
                glow,
                enchantmentResult.enchantments(),
                flags,
                loadPotion(menuId, itemId, section.getConfigurationSection("potion")),
                section.getString("permission", ""),
                section.getString("permission-message", ""),
                loadSound(menuId, itemId, section, "click-sound", section.getString("sound", "")),
                loadRequirements(section.getConfigurationSection("view-requirements")),
                loadRequirements(section.getConfigurationSection("click-requirements")),
                clicks,
                fallbackActions
        );
    }

    private EnchantmentLoadResult loadEnchantments(String menuId, String itemId, ConfigurationSection section) {
        if (section == null) {
            return new EnchantmentLoadResult(Map.of(), false);
        }

        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        boolean glintOverride = false;
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("glint_override")) {
                glintOverride = section.getInt(key, 0) > 0 || section.getBoolean(key, false);
                continue;
            }

            Enchantment enchantment = resolveEnchantment(key);
            if (enchantment == null) {
                fail("menus/" + menuId + ".yml -> items." + itemId + ".enchantments." + key, "unknown enchantment");
                continue;
            }
            enchantments.put(enchantment, Math.max(1, section.getInt(key, 1)));
        }
        return new EnchantmentLoadResult(enchantments, glintOverride);
    }

    private Enchantment resolveEnchantment(String rawName) {
        String normalized = rawName.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        Enchantment direct = Enchantment.getByName(normalized);
        if (direct != null) {
            return direct;
        }

        String legacyName = switch (normalized) {
            case "PROTECTION" -> "PROTECTION_ENVIRONMENTAL";
            case "FIRE_PROTECTION" -> "PROTECTION_FIRE";
            case "FEATHER_FALLING" -> "PROTECTION_FALL";
            case "BLAST_PROTECTION" -> "PROTECTION_EXPLOSIONS";
            case "PROJECTILE_PROTECTION" -> "PROTECTION_PROJECTILE";
            case "RESPIRATION" -> "OXYGEN";
            case "AQUA_AFFINITY" -> "WATER_WORKER";
            case "SHARPNESS" -> "DAMAGE_ALL";
            case "SMITE" -> "DAMAGE_UNDEAD";
            case "BANE_OF_ARTHROPODS" -> "DAMAGE_ARTHROPODS";
            case "KNOCKBACK" -> "KNOCKBACK";
            case "FIRE_ASPECT" -> "FIRE_ASPECT";
            case "LOOTING" -> "LOOT_BONUS_MOBS";
            case "SWEEPING", "SWEEPING_EDGE" -> "SWEEPING_EDGE";
            case "EFFICIENCY" -> "DIG_SPEED";
            case "SILK_TOUCH" -> "SILK_TOUCH";
            case "UNBREAKING" -> "DURABILITY";
            case "FORTUNE" -> "LOOT_BONUS_BLOCKS";
            case "POWER" -> "ARROW_DAMAGE";
            case "PUNCH" -> "ARROW_KNOCKBACK";
            case "FLAME" -> "ARROW_FIRE";
            case "INFINITY" -> "ARROW_INFINITE";
            case "LUCK_OF_THE_SEA" -> "LUCK";
            case "LURE" -> "LURE";
            case "MENDING" -> "MENDING";
            case "VANISHING_CURSE", "CURSE_OF_VANISHING" -> "VANISHING_CURSE";
            case "BINDING_CURSE", "CURSE_OF_BINDING" -> "BINDING_CURSE";
            default -> normalized;
        };
        return Enchantment.getByName(legacyName);
    }

    private record EnchantmentLoadResult(Map<Enchantment, Integer> enchantments, boolean glintOverride) {
    }

    private ItemFlag resolveItemFlag(String rawFlag) {
        String normalized = rawFlag.toUpperCase(Locale.ROOT).replace('-', '_').trim();
        ItemFlag direct = itemFlagByName(normalized);
        if (direct != null) {
            return direct;
        }
        if (normalized.equals("HIDE_POTION_EFFECTS")) {
            return itemFlagByName("HIDE_ADDITIONAL_TOOLTIP");
        }
        if (normalized.equals("HIDE_ADDITIONAL_TOOLTIP")) {
            return itemFlagByName("HIDE_POTION_EFFECTS");
        }
        return null;
    }

    private ItemFlag itemFlagByName(String name) {
        try {
            return ItemFlag.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private List<Integer> loadSlots(String menuId, String itemId, Object rawSlot, int menuSize) {
        if (rawSlot == null) {
            return List.of();
        }

        List<Integer> slots = new ArrayList<>();
        if (rawSlot instanceof Number number) {
            slots.add(number.intValue());
        } else if (rawSlot instanceof List<?> list) {
            for (Object value : list) {
                addSlot(menuId, itemId, String.valueOf(value), menuSize, slots);
            }
        } else {
            for (String value : String.valueOf(rawSlot).split(",")) {
                addSlot(menuId, itemId, value, menuSize, slots);
            }
        }

        List<Integer> normalized = slots.stream().distinct().toList();
        for (int slot : normalized) {
            if (slot < 0 || slot >= menuSize) {
                fail("menus/" + menuId + ".yml -> items." + itemId + ".slot", "invalid slot " + slot + " for menu size " + menuSize);
                return List.of();
            }
        }
        return normalized;
    }

    private void addSlot(String menuId, String itemId, String rawSlot, int menuSize, List<Integer> slots) {
        try {
            slots.add(Integer.parseInt(rawSlot.trim()));
        } catch (NumberFormatException exception) {
            fail("menus/" + menuId + ".yml -> items." + itemId + ".slot", "invalid slot '" + rawSlot + "' for menu size " + menuSize);
        }
    }

    private PotionDefinition loadPotion(String menuId, String itemId, ConfigurationSection section) {
        if (section == null) {
            return PotionDefinition.empty();
        }

        Color color = null;
        String rawColor = section.getString("color", "");
        if (!rawColor.isBlank()) {
            color = parseColor("menus/" + menuId + ".yml -> items." + itemId + ".potion.color", rawColor);
        }

        PotionType baseType = null;
        String rawBase = section.getString("base", section.getString("base-type", ""));
        if (!rawBase.isBlank()) {
            try {
                baseType = PotionType.valueOf(rawBase.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                fail("menus/" + menuId + ".yml -> items." + itemId + ".potion.base", "invalid potion base '" + rawBase + "'");
            }
        }

        List<PotionEffectDefinition> effects = new ArrayList<>();
        int index = 0;
        for (Map<?, ?> rawEffect : section.getMapList("effects")) {
            String path = "menus/" + menuId + ".yml -> items." + itemId + ".potion.effects[" + index + "]";
            String rawType = String.valueOf(rawEffect.containsKey("type") ? rawEffect.get("type") : "");
            PotionEffectType type = PotionEffectType.getByName(rawType.toUpperCase(Locale.ROOT));
            if (type == null) {
                fail(path + ".type", "invalid potion effect '" + rawType + "'");
                index++;
                continue;
            }

            effects.add(new PotionEffectDefinition(
                    type,
                    asInt(rawEffect.get("duration"), 600),
                    asInt(rawEffect.get("amplifier"), 0),
                    asBoolean(rawEffect.get("ambient"), false),
                    asBoolean(rawEffect.get("particles"), true),
                    asBoolean(rawEffect.get("icon"), true)
            ));
            index++;
        }

        return new PotionDefinition(color, baseType, effects);
    }

    private Color parseColor(String path, String rawColor) {
        String value = rawColor.trim().replace("#", "");
        if (!value.matches("[A-Fa-f0-9]{6}")) {
            fail(path, "invalid hex color '" + rawColor + "'");
            return null;
        }
        int rgb = Integer.parseInt(value, 16);
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private String readOpenCommand(YamlConfiguration config, String id) {
        Object value = config.get("open-command");
        if (value instanceof Boolean enabled && !enabled) {
            return null;
        }
        if (value instanceof String command) {
            String normalized = command.trim();
            return normalized.isBlank() ? null : normalized.replaceFirst("^/", "").toLowerCase(Locale.ROOT);
        }
        return id;
    }

    private Map<MenuClickType, MenuClickDefinition> loadClicks(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<MenuClickType, MenuClickDefinition> clicks = new EnumMap<>(MenuClickType.class);
        for (String key : section.getKeys(false)) {
            ConfigurationSection clickSection = section.getConfigurationSection(key);
            if (clickSection == null) {
                continue;
            }
            clicks.put(
                    MenuClickType.fromConfig(key),
                    new MenuClickDefinition(
                            parseActions(clickSection.getStringList("commands")),
                            loadRequirements(clickSection.getConfigurationSection("requirements")),
                            loadClickSound(clickSection, key)
                    )
            );
        }
        return clicks;
    }

    private SoundEffect loadClickSound(ConfigurationSection section, String clickKey) {
        return SoundEffect.parse(section.getString("sound", ""), "clicks." + clickKey + ".sound", report::warning);
    }

    private MenuAnimationDefinition loadOpenAnimation(String menuId, ConfigurationSection section) {
        if (section == null) {
            return MenuAnimationDefinition.none();
        }

        MenuAnimationType type = MenuAnimationType.from(section.getString("type", "NONE"));
        int interval = Math.max(1, section.getInt("interval", 1));
        Material material = Material.matchMaterial(section.getString("material", "BLACK_STAINED_GLASS_PANE").toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            fail("menus/" + menuId + ".yml -> animation.open.material", "invalid animation material '" + section.getString("material") + "'");
            material = Material.BLACK_STAINED_GLASS_PANE;
        }

        SoundEffect sound = loadStrictSound(section.getString("sound", ""), "menus/" + menuId + ".yml -> animation.open.sound");

        return new MenuAnimationDefinition(type, interval, material, sound.sound(), sound.volume(), sound.pitch(), List.of());
    }

    private MenuAnimationDefinition loadScriptAnimation(String menuId, YamlConfiguration config) {
        List<MenuAnimationFrame> frames = new ArrayList<>();
        for (Map<?, ?> rawFrame : config.getMapList("animation")) {
            int tick = asInt(rawFrame.get("tick"), 0);
            Object opcodes = rawFrame.get("opcodes");
            List<MenuAnimationPlacement> placements = new ArrayList<>();
            if (opcodes instanceof List<?> opcodeList) {
                for (Object opcode : opcodeList) {
                    parseOpcode(menuId, opcode).ifPresent(placements::add);
                }
            }
            Object rawSound = rawFrame.containsKey("sound") ? rawFrame.get("sound") : "";
            SoundEffect frameSound = loadStrictSound(String.valueOf(rawSound), "menus/" + menuId + ".yml -> animation.tick." + tick + ".sound");
            frames.add(new MenuAnimationFrame(tick, placements, frameSound));
        }
        frames.sort(java.util.Comparator.comparingInt(MenuAnimationFrame::tick));
        String rawAnimationSound = config.getString("animation-sound", "");
        SoundEffect sound = loadStrictSound(rawAnimationSound, "menus/" + menuId + ".yml -> animation-sound");
        return new MenuAnimationDefinition(
                MenuAnimationType.SCRIPT,
                Math.max(1, config.getInt("animation-interval", 1)),
                Material.BLACK_STAINED_GLASS_PANE,
                sound.sound(),
                sound.volume(),
                sound.pitch(),
                frames
        );
    }

    private java.util.Optional<MenuAnimationPlacement> parseOpcode(String menuId, Object opcode) {
        if (opcode instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT).replace(":", "");
                if (key.equals("set")) {
                    return parsePlacement(menuId, String.valueOf(entry.getValue()));
                }
                if (key.equals("remove")) {
                    return parseRemove(menuId, String.valueOf(entry.getValue()));
                }
            }
        }
        if (opcode instanceof String line && line.startsWith("set:")) {
            return parsePlacement(menuId, line.substring("set:".length()).trim());
        }
        if (opcode instanceof String line && line.startsWith("remove:")) {
            return parseRemove(menuId, line.substring("remove:".length()).trim());
        }
        fail("menus/" + menuId + ".yml -> animation.opcodes", "unknown animation opcode '" + opcode + "'");
        return java.util.Optional.empty();
    }

    private java.util.Optional<MenuAnimationPlacement> parsePlacement(String menuId, String value) {
        String[] parts = value.trim().split("\\s+", 2);
        if (parts.length < 2) {
            fail("menus/" + menuId + ".yml -> animation.opcodes.set", "expected '<itemId> <slots>'");
            return java.util.Optional.empty();
        }

        List<Integer> slots = new ArrayList<>();
        for (String rawSlot : parts[1].split(",")) {
            try {
                slots.add(Integer.parseInt(rawSlot.trim()));
            } catch (NumberFormatException exception) {
                fail("menus/" + menuId + ".yml -> animation.opcodes.set", "invalid slot '" + rawSlot + "'");
            }
        }
        return java.util.Optional.of(MenuAnimationPlacement.set(parts[0].toLowerCase(Locale.ROOT), slots));
    }

    private java.util.Optional<MenuAnimationPlacement> parseRemove(String menuId, String value) {
        List<Integer> slots = new ArrayList<>();
        for (String rawSlot : value.trim().split(",")) {
            if (rawSlot.isBlank()) {
                continue;
            }
            try {
                slots.add(Integer.parseInt(rawSlot.trim()));
            } catch (NumberFormatException exception) {
                fail("menus/" + menuId + ".yml -> animation.opcodes.remove", "invalid slot '" + rawSlot + "'");
            }
        }
        return java.util.Optional.of(MenuAnimationPlacement.remove(slots));
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<Requirement> loadRequirements(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }

        List<Requirement> requirements = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection requirement = section.getConfigurationSection(key);
            if (requirement == null) {
                continue;
            }
            String type = requirement.getString("type", "permission").toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                requirements.add(new Requirement(
                        RequirementType.valueOf(type),
                        requirement.getString("input", requirement.getString("permission", "")),
                        requirement.getString("expected", "")
                ));
            } catch (IllegalArgumentException exception) {
                fail("requirements." + key, "unknown requirement type '" + type + "'");
            }
        }
        return requirements;
    }

    private List<MenuAction> parseActions(List<String> actions) {
        return actions.stream()
                .map(MenuAction::parse)
                .toList();
    }

    private SoundEffect loadSound(String menuId, String itemId, ConfigurationSection section, String primaryPath, String fallbackRaw) {
        String raw = section.getString(primaryPath, fallbackRaw);
        return loadStrictSound(raw, "menus/" + menuId + ".yml -> items." + itemId + "." + primaryPath);
    }

    private int normalizeSize(int requested, String menuId) {
        if (requested >= 9 && requested <= 54 && requested % 9 == 0) {
            return requested;
        }
        fail("menus/" + menuId + ".yml -> size", "invalid size " + requested + "; expected 9,18,27,36,45,54");
        return settings.defaultMenuSize();
    }

    private void warn(String path, String message) {
        String warning = path + ": " + message;
        report.warning(warning);
        plugin.getLogger().warning(warning);
    }

    private void fail(String path, String message) {
        String error = path + ": " + message;
        currentErrors.add(error);
        report.error(error);
        plugin.getLogger().severe("[LiteMenus] [ERROR] " + error);
    }

    private void error(String path, String message) {
        String error = path + ": " + message;
        report.error(error);
        plugin.getLogger().severe("[LiteMenus] [ERROR] " + error);
    }

    private SoundEffect loadStrictSound(String raw, String path) {
        try {
            return SoundEffect.parseStrict(raw, path);
        } catch (MenuValidationException exception) {
            fail(path, exception.getMessage().contains(": ") ? exception.getMessage().substring(exception.getMessage().indexOf(": ") + 2) : exception.getMessage());
            return SoundEffect.none();
        }
    }
}
