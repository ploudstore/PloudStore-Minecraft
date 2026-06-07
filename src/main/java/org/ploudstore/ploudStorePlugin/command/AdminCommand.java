package org.ploudstore.ploudStorePlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final PloudStorePlugin plugin;
    private final ExecutedCache executedCache;

    public AdminCommand(PloudStorePlugin plugin, ExecutedCache executedCache) {
        this.plugin = plugin;
        this.executedCache = executedCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ploudstore.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                sender.sendMessage("§a[PloudStore] Config reloaded.");
                break;
            case "status":
                sender.sendMessage("§e[PloudStore] Status:");
                sender.sendMessage("§7  Executed cache (in-memory): §f" + executedCache.size() + " recent command(s)");
                break;
            case "forcecheck":
                sender.sendMessage("§a[PloudStore] Forcing command check...");
                plugin.getPluginScheduler().runAsync(new Runnable() {
                    public void run() { plugin.getCommandProcessor().performCheck(); }
                });
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e[PloudStore] Commands:");
        sender.sendMessage("§7  /ploudstore reload §f— Reload config");
        sender.sendMessage("§7  /ploudstore status §f— Show cache status");
        sender.sendMessage("§7  /ploudstore forcecheck §f— Force fetch pending commands now");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "status", "forcecheck");
        }
        return Collections.emptyList();
    }
}
