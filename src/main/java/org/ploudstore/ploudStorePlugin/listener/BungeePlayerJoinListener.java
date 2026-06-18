package org.ploudstore.ploudStorePlugin.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.ploudstore.ploudStorePlugin.PloudStoreBungeePlugin;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;

import java.util.Locale;

public class BungeePlayerJoinListener implements Listener {

    private final PloudStoreBungeePlugin plugin;

    public BungeePlayerJoinListener(PloudStoreBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final ProxiedPlayer player = event.getPlayer();

        UpdateChecker updater = plugin.getUpdateChecker();
        if (updater != null && updater.isUpdateAvailable()
                && player.hasPermission("ploudstore.admin")) {
            player.sendMessage(net.md_5.bungee.api.ChatColor.YELLOW
                    + "[PloudStore] Nova versão v" + updater.getLatestVersion()
                    + " disponível! Acede ao GitHub para descarregar.");
        }

        CommandProcessor processor = plugin.getCommandProcessor();
        if (processor == null) return;

        final String name = player.getName();
        if (!processor.getQueuedPlayers().contains(name.toLowerCase(Locale.ROOT))) return;

        plugin.getLogger().fine("[PloudStore] " + name + " joined (proxy) with queued commands — requesting check.");
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            public void run() { plugin.getCommandProcessor().requestCheck(); }
        }, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
}
