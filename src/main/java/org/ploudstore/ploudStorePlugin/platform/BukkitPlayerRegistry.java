package org.ploudstore.ploudStorePlugin.platform;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Locale;

public class BukkitPlayerRegistry implements PlayerRegistry {

    @Override
    public boolean isOnline(String name) {
        return findPlayer(name) != null;
    }

    @Override
    public int getFreeSlots(String name) {
        Player player = findPlayer(name);
        if (player == null) return 0;
        ItemStack[] contents = Arrays.copyOfRange(player.getInventory().getContents(), 0, 36);
        int free = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) free++;
        }
        return free;
    }

    @Override
    public void sendMessage(String name, String message) {
        Player player = findPlayer(name);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    private Player findPlayer(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).equals(lower)) return p;
        }
        return null;
    }
}
