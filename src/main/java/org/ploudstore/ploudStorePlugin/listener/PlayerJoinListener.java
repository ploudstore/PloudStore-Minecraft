package org.ploudstore.ploudStorePlugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;

import java.util.Locale;

public class PlayerJoinListener implements Listener {

    private final PloudStorePlugin plugin;

    public PlayerJoinListener(PloudStorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final org.bukkit.entity.Player player = event.getPlayer();

        UpdateChecker updater = plugin.getUpdateChecker();
        if (updater != null && updater.isUpdateAvailable()
                && player.hasPermission("ploudstore.admin")) {
            player.sendMessage("§e[PloudStore] Nova versão v" + updater.getLatestVersion()
                    + " disponível! Reinicia o servidor para aplicar o update.");
        }

        CommandProcessor processor = plugin.getCommandProcessor();
        if (processor == null) return;

        String name = player.getName();
        if (!processor.getQueuedPlayers().contains(name.toLowerCase(Locale.ROOT))) return;

        plugin.getPluginLogger().debug("[PloudStore] " + name + " joined with queued commands — requesting check.");
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
            public void run() { plugin.getCommandProcessor().requestCheck(); }
        }, 20L);
    }
}
