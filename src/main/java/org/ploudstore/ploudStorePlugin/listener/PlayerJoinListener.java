package org.ploudstore.ploudStorePlugin.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        var player = event.getPlayer();

        UpdateChecker updater = plugin.getUpdateChecker();
        if (updater != null && updater.isUpdateAvailable()
                && player.hasPermission("ploudstore.admin")) {
            player.sendMessage(Component.text(
                    "[PloudStore] Nova versão v" + updater.getLatestVersion()
                    + " disponível! Reinicia o servidor para aplicar o update.",
                    NamedTextColor.YELLOW));
        }

        CommandProcessor processor = plugin.getCommandProcessor();
        if (processor == null) return;

        String name = player.getName();
        if (!processor.getQueuedPlayers().contains(name.toLowerCase(Locale.ROOT))) return;

        plugin.getLogger().info("[PloudStore] " + name + " joined with queued commands — requesting check.");
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                processor::requestCheck, 20L);
    }
}
