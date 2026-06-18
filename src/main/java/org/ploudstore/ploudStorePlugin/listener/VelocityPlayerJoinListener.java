package org.ploudstore.ploudStorePlugin.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ploudstore.ploudStorePlugin.PloudStoreVelocityPlugin;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VelocityPlayerJoinListener {

    private final PloudStoreVelocityPlugin plugin;

    public VelocityPlayerJoinListener(PloudStoreVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        final Player player = event.getPlayer();

        UpdateChecker updater = plugin.getUpdateChecker();
        if (updater != null && updater.isUpdateAvailable()
                && player.hasPermission("ploudstore.admin")) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    "§e[PloudStore] Nova versão v" + updater.getLatestVersion()
                    + " disponível! Acede ao GitHub para descarregar."));
        }

        CommandProcessor processor = plugin.getCommandProcessor();
        if (processor == null) return;

        final String name = player.getUsername();
        if (!processor.getQueuedPlayers().contains(name.toLowerCase(Locale.ROOT))) return;

        plugin.getProxy().getScheduler()
                .buildTask(plugin, new Runnable() {
                    public void run() { plugin.getCommandProcessor().requestCheck(); }
                })
                .delay(1, TimeUnit.SECONDS)
                .schedule();
    }
}
