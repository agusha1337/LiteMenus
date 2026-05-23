package ru.agusha.menus.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import ru.agusha.menus.placeholder.PlaceholderService;
import ru.agusha.menus.menu.action.MenuAction;
import ru.agusha.menus.util.Text;

public record MenuItemDefinition(
        String id,
        List<Integer> slots,
        Material material,
        String headTexture,
        int amount,
        int damage,
        String name,
        List<String> lore,
        boolean glow,
        Map<Enchantment, Integer> enchantments,
        List<ItemFlag> flags,
        PotionDefinition potion,
        String permission,
        String permissionMessage,
        SoundEffect clickSound,
        List<Requirement> viewRequirements,
        List<Requirement> clickRequirements,
        Map<MenuClickType, MenuClickDefinition> clicks,
        List<MenuAction> fallbackActions
) {

    private static final Pattern TEXTURE_URL = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    public int slot() {
        return slots.isEmpty() ? -1 : slots.get(0);
    }

    public ItemStack createItemStack(PlaceholderService placeholders, org.bukkit.entity.Player player) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (name != null && !name.isBlank()) {
            meta.setDisplayName(Text.color(placeholders.apply(player, name)));
        }
        if (!lore.isEmpty()) {
            meta.setLore(Text.color(placeholders.apply(player, lore)));
        }
        if (!flags.isEmpty()) {
            meta.addItemFlags(flags.toArray(ItemFlag[]::new));
        }
        if (!enchantments.isEmpty()) {
            enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, Math.max(1, level), true));
        }
        if (glow && enchantments.isEmpty()) {
            Enchantment glowEnchant = Enchantment.getByName("UNBREAKING");
            if (glowEnchant == null) {
                glowEnchant = Enchantment.getByName("DURABILITY");
            }
            if (glowEnchant != null) {
                meta.addEnchant(glowEnchant, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        applyPotionMeta(meta);
        applyHeadTexture(meta);

        item.setItemMeta(meta);
        if (damage > 0 && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) Math.min(damage, item.getType().getMaxDurability()));
        }
        return item;
    }

    @SuppressWarnings("deprecation")
    private void applyPotionMeta(ItemMeta meta) {
        if (!(meta instanceof PotionMeta potionMeta) || potion == null || !potion.hasData()) {
            return;
        }

        if (potion.color() != null) {
            potionMeta.setColor(potion.color());
        }
        if (potion.baseType() != null) {
            potionMeta.setBasePotionData(new PotionData(potion.baseType()));
        }
        for (PotionEffectDefinition effect : potion.effects()) {
            potionMeta.addCustomEffect(new PotionEffect(
                    effect.type(),
                    effect.duration(),
                    effect.amplifier(),
                    effect.ambient(),
                    effect.particles(),
                    effect.icon()
            ), true);
        }
    }

    private void applyHeadTexture(ItemMeta meta) {
        if (!(meta instanceof SkullMeta) || headTexture == null || headTexture.isBlank()) {
            return;
        }

        try {
            if (applyModernHeadTexture(meta)) {
                return;
            }
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.nameUUIDFromBytes(headTexture.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "LiteMenus");
            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", headTexture);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Older and newer Bukkit versions hide skull profile internals differently.
            // If reflection fails, the head simply stays as a normal PLAYER_HEAD.
        }
    }

    private boolean applyModernHeadTexture(ItemMeta meta) {
        String textureUrl = decodeTextureUrl();
        if (textureUrl.isBlank()) {
            return false;
        }

        try {
            Object profile = Bukkit.class
                    .getMethod("createPlayerProfile", UUID.class, String.class)
                    .invoke(null, UUID.nameUUIDFromBytes(headTexture.getBytes(StandardCharsets.UTF_8)), "LiteMenus");
            Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            textures.getClass().getMethod("setSkin", URL.class).invoke(textures, new URL(textureUrl));
            invokeOneArg(profile, "setTextures", textures);
            return invokeOneArg(meta, "setOwnerProfile", profile);
        } catch (ReflectiveOperationException | java.net.MalformedURLException | RuntimeException exception) {
            return false;
        }
    }

    private boolean invokeOneArg(Object target, String methodName, Object argument) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                method.invoke(target, argument);
                return true;
            }
        }
        return false;
    }

    private String decodeTextureUrl() {
        try {
            String json = new String(Base64.getDecoder().decode(headTexture), StandardCharsets.UTF_8);
            Matcher matcher = TEXTURE_URL.matcher(json);
            return matcher.find() ? matcher.group(1) : "";
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    public List<MenuAction> actionsFor(MenuClickType clickType) {
        MenuClickDefinition exact = clicks.get(clickType);
        if (exact != null && !exact.actions().isEmpty()) {
            return exact.actions();
        }

        MenuClickDefinition any = clicks.get(MenuClickType.ANY);
        if (any != null && !any.actions().isEmpty()) {
            return any.actions();
        }

        return fallbackActions;
    }

    public SoundEffect soundFor(MenuClickType clickType) {
        MenuClickDefinition exact = clicks.get(clickType);
        if (exact != null && exact.sound().enabled()) {
            return exact.sound();
        }

        MenuClickDefinition any = clicks.get(MenuClickType.ANY);
        if (any != null && any.sound().enabled()) {
            return any.sound();
        }

        return clickSound;
    }

    public List<Requirement> requirementsFor(MenuClickType clickType) {
        MenuClickDefinition exact = clicks.get(clickType);
        if (exact != null && !exact.requirements().isEmpty()) {
            return exact.requirements();
        }

        MenuClickDefinition any = clicks.get(MenuClickType.ANY);
        if (any != null && !any.requirements().isEmpty()) {
            return any.requirements();
        }

        return clickRequirements;
    }
}
