package ru.agusha.menus.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class MenuSessionManager {

    private final Map<UUID, MenuDefinition> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private final Map<UUID, Boolean> animating = new HashMap<>();

    public void open(Player player, MenuDefinition menu) {
        sessions.put(player.getUniqueId(), menu);
    }

    public Optional<MenuDefinition> get(Player player) {
        return Optional.ofNullable(sessions.get(player.getUniqueId()));
    }

    public boolean isAnimating(Player player) {
        return animating.getOrDefault(player.getUniqueId(), false);
    }

    public void startAnimation(Player player) {
        animating.put(player.getUniqueId(), true);
    }

    public void setAnimationTask(Player player, BukkitTask task) {
        cancelAnimation(player);
        animationTasks.put(player.getUniqueId(), task);
        animating.put(player.getUniqueId(), true);
    }

    public void finishAnimation(Player player) {
        BukkitTask task = animationTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        animating.put(player.getUniqueId(), false);
    }

    public void cancelAnimation(Player player) {
        BukkitTask task = animationTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        animating.remove(player.getUniqueId());
    }

    public void close(Player player) {
        cancelAnimation(player);
        sessions.remove(player.getUniqueId());
    }

    public void clear() {
        animationTasks.values().forEach(BukkitTask::cancel);
        animationTasks.clear();
        animating.clear();
        sessions.clear();
    }
}
