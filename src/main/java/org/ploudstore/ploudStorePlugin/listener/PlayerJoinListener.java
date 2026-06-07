package org.ploudstore.ploudStorePlugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;

import java.util.Locale;

public class PlayerJoinListener implements Listener {

    private final PloudStorePlugin plugin;

    public PlayerJoinListener(PloudStorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        CommandProcessor processor = plugin.getCommandProcessor();
        if (processor == null) return;

        String name = event.getPlayer().getName();
        if (!processor.getQueuedPlayers().contains(name.toLowerCase(Locale.ROOT))) return;

        plugin.getLogger().info("[PloudStore] " + name + " joined with queued commands — requesting check.");
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                processor::requestCheck, 20L);
    }
}
