package ru.agusha.menus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.agusha.menus.animation.MenuAnimationService;
import ru.agusha.menus.command.LiteMenusCommand;
import ru.agusha.menus.command.MenuCommandManager;
import ru.agusha.menus.config.DefaultResources;
import ru.agusha.menus.config.Messages;
import ru.agusha.menus.config.PluginSettings;
import ru.agusha.menus.listener.MenuListener;
import ru.agusha.menus.menu.MenuLoader;
import ru.agusha.menus.menu.MenuManager;
import ru.agusha.menus.menu.MenuSessionManager;
import ru.agusha.menus.placeholder.PlaceholderService;

public final class LiteMenus extends JavaPlugin {

    private PluginSettings settings;
    private Messages messages;
    private PlaceholderService placeholders;
    private MenuAnimationService animations;
    private MenuManager menuManager;
    private MenuSessionManager sessionManager;
    private MenuCommandManager menuCommandManager;
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        saveDefaultFiles();

        this.settings = new PluginSettings(this);
        this.messages = new Messages(this);
        settings.reload();
        messages.reload();
        this.placeholders = new PlaceholderService(this, settings);
        placeholders.reload();
        this.animations = new MenuAnimationService(this, placeholders);
        this.sessionManager = new MenuSessionManager();
        this.menuManager = new MenuManager(this, new MenuLoader(this, settings), placeholders, animations);
        this.menuCommandManager = new MenuCommandManager(this, settings, menuManager, sessionManager, messages);

        reloadAll();

        LiteMenusCommand command = new LiteMenusCommand(
                this,
                settings,
                messages,
                placeholders,
                menuManager,
                sessionManager,
                menuCommandManager
        );
        Objects.requireNonNull(getCommand("litemenus"), "нету комманды")
                .setExecutor(command);
        Objects.requireNonNull(getCommand("litemenus"), "нету комманды")
                .setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new MenuListener(sessionManager, menuManager, placeholders, messages), this);
        startUpdateTask();

        getLogger().info("Загруженно " + menuManager.getMenus().size() + " меню");

    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (menuCommandManager != null) {
            menuCommandManager.unregisterAll();
        }
        if (sessionManager != null) {
            sessionManager.clear();
        }
    }

    public void reloadAll() {
        saveDefaultFiles();
        settings.reload();
        messages.reload();
        placeholders.reload();
        for (Player player : getServer().getOnlinePlayers()) {
            if (sessionManager.get(player).isPresent()) {
                player.closeInventory();
            }
        }
        sessionManager.clear();
        menuManager.reload();
        menuCommandManager.reload();
    }

    private void startUpdateTask() {
        this.updateTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            private long tick;

            @Override
            public void run() {
                tick++;
                for (Player player : getServer().getOnlinePlayers()) {
                    sessionManager.get(player)
                            .filter(menu -> menu.update() && tick % menu.updateInterval() == 0)
                            .filter(menu -> !sessionManager.isAnimating(player))
                            .ifPresent(menu -> menuManager.refresh(player, menu));
                }
            }
        }, 1L, 1L);
    }

    private void saveDefaultFiles() {
        saveBundledOrFallback("config.yml");
        saveBundledOrFallback("messages.yml");
        File menusFolder = new File(getDataFolder(), "menus");
        if (!menusFolder.exists() && !menusFolder.mkdirs()) {
            getLogger().warning("папка не создана :( " + menusFolder.getAbsolutePath());
        }
    }

    private void saveBundledOrFallback(String path) {
        File target = new File(getDataFolder(), path);
        if (target.exists()) {
            return;
        }

        if (getResource(path) != null) {
            saveResource(path, false);
            return;
        }
    }
}
