package org.ploudstore.ploudStorePlugin.dispatch;

import org.bukkit.Bukkit;

public class BukkitCommandDispatcher implements CommandDispatcher {
    @Override
    public boolean dispatch(String command) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
