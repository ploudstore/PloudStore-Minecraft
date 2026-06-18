package org.ploudstore.ploudStorePlugin.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ploudstore.ploudStorePlugin.PloudStoreVelocityPlugin;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;

public class VelocityAdminCommand implements SimpleCommand {

    private final PloudStoreVelocityPlugin plugin;
    private final ExecutedCache executedCache;

    public VelocityAdminCommand(PloudStoreVelocityPlugin plugin, ExecutedCache executedCache) {
        this.plugin        = plugin;
        this.executedCache = executedCache;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                send(sender, "§a[PloudStore] Config reloaded.");
                break;
            case "status":
                send(sender, "§e[PloudStore] Status:");
                send(sender, "§7  Executed cache (in-memory): §f" + executedCache.size() + " recent command(s)");
                break;
            case "forcecheck":
                send(sender, "§a[PloudStore] Forcing command check...");
                plugin.getProxy().getScheduler()
                        .buildTask(plugin, new Runnable() {
                            public void run() { plugin.getCommandProcessor().performCheck(); }
                        })
                        .schedule();
                break;
            default:
                sendHelp(sender);
                break;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("ploudstore.admin");
    }

    private void sendHelp(CommandSource sender) {
        send(sender, "§e[PloudStore] Commands:");
        send(sender, "§7  /ploudstore reload §f— Reload config");
        send(sender, "§7  /ploudstore status §f— Show cache status");
        send(sender, "§7  /ploudstore forcecheck §f— Force fetch pending commands now");
    }

    private void send(CommandSource sender, String message) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }
}
