package org.ploudstore.ploudStorePlugin.dispatch;

import com.velocitypowered.api.proxy.ProxyServer;

public class VelocityCommandDispatcher implements CommandDispatcher {

    private final ProxyServer proxy;

    public VelocityCommandDispatcher(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean dispatch(String command) {
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
        return true; // Velocity doesn't return a sync result
    }
}
