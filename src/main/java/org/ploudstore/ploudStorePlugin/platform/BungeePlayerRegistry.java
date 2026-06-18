package org.ploudstore.ploudStorePlugin.platform;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlayerRegistry implements PlayerRegistry {

    private final ProxyServer proxy;

    public BungeePlayerRegistry(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isOnline(String name) {
        return proxy.getPlayer(name) != null;
    }

    @Override
    public int getFreeSlots(String name) {
        return 0; // BungeeCord has no inventory concept
    }

    @Override
    public void sendMessage(String name, String message) {
        ProxiedPlayer player = proxy.getPlayer(name);
        if (player != null) {
            // message already has § color codes applied by CommandProcessor
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }
}
