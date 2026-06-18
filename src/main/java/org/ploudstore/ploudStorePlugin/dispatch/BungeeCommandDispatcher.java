package org.ploudstore.ploudStorePlugin.dispatch;

import net.md_5.bungee.api.ProxyServer;

public class BungeeCommandDispatcher implements CommandDispatcher {

    private final ProxyServer proxy;

    public BungeeCommandDispatcher(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public boolean dispatch(String command) {
        return proxy.getPluginManager().dispatchCommand(proxy.getConsole(), command);
    }
}
