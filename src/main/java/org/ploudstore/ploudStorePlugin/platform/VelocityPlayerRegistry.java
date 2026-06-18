package org.ploudstore.ploudStorePlugin.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;

public class VelocityPlayerRegistry implements PlayerRegistry {

    private final ProxyServer proxy;

    public VelocityPlayerRegistry(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean isOnline(String name) {
        return proxy.getPlayer(name).isPresent();
    }

    @Override
    public int getFreeSlots(String name) {
        return 0; // Velocity has no inventory concept
    }

    @Override
    public void sendMessage(String name, String message) {
        Optional<Player> player = proxy.getPlayer(name);
        if (player.isPresent()) {
            player.get().sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }
    }
}
