package org.ploudstore.ploudStorePlugin.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.ploudstore.ploudStorePlugin.PloudStoreBungeePlugin;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;

public class BungeeAdminCommand extends Command {

    private final PloudStoreBungeePlugin plugin;
    private final ExecutedCache executedCache;

    public BungeeAdminCommand(PloudStoreBungeePlugin plugin, ExecutedCache executedCache) {
        super("ploudstore", "ploudstore.admin", "plstore-admin");
        this.plugin = plugin;
        this.executedCache = executedCache;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ploudstore.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "[PloudStore] Config reloaded.");
                break;
            case "status":
                sender.sendMessage(ChatColor.YELLOW + "[PloudStore] Status:");
                sender.sendMessage(ChatColor.GRAY + "  Executed cache (in-memory): "
                        + ChatColor.WHITE + executedCache.size() + " recent command(s)");
                break;
            case "forcecheck":
                sender.sendMessage(ChatColor.GREEN + "[PloudStore] Forcing command check...");
                plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                    public void run() { plugin.getCommandProcessor().performCheck(); }
                });
                break;
            default:
                sendHelp(sender);
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "[PloudStore] Commands:");
        sender.sendMessage(ChatColor.GRAY + "  /ploudstore reload " + ChatColor.WHITE + "— Reload config");
        sender.sendMessage(ChatColor.GRAY + "  /ploudstore status " + ChatColor.WHITE + "— Show cache status");
        sender.sendMessage(ChatColor.GRAY + "  /ploudstore forcecheck " + ChatColor.WHITE + "— Force fetch pending commands now");
    }
}
