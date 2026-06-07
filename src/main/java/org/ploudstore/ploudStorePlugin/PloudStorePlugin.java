package org.ploudstore.ploudStorePlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.ploudstore.ploudStorePlugin.api.ApiClient;
import org.ploudstore.ploudStorePlugin.command.AdminCommand;
import org.ploudstore.ploudStorePlugin.command.StoreCommand;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.listener.PlayerJoinListener;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;

public final class PloudStorePlugin extends JavaPlugin {

    private ExecutedCache executedCache;
    private CommandProcessor commandProcessor;
    private ApiClient apiClient;
    private BukkitTask evictTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        executedCache = new ExecutedCache();

        if (!startCommandProcessor()) return;

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        AdminCommand adminCmd = new AdminCommand(this, executedCache);
        var cmd = getCommand("ploudstore");
        if (cmd != null) {
            cmd.setExecutor(adminCmd);
            cmd.setTabCompleter(adminCmd);
        }

        var storeCmd = getCommand("plstore");
        if (storeCmd != null) {
            storeCmd.setExecutor(new StoreCommand(this));
        }

        getLogger().info("[PloudStore] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (evictTask != null) { evictTask.cancel(); evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); }
        getLogger().info("[PloudStore] Plugin disabled.");
    }

    public CommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void reload() {
        if (evictTask != null) { evictTask.cancel(); evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); commandProcessor = null; }
        reloadConfig();
        startCommandProcessor();
        getLogger().info("[PloudStore] Reloaded.");
    }

    private boolean startCommandProcessor() {
        String secretKey = getConfig().getString("api.secret-key", "").strip();
        if (secretKey.isEmpty() || secretKey.equals("your-secret-key-here")) {
            getLogger().severe("[PloudStore] api.secret-key is not configured in config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        String baseUrl        = getConfig().getString("api.base-url", "https://command.ploudstore.com");
        int fallbackNextCheck = getConfig().getInt("api.fallback-next-check-seconds", 60);
        int timeout           = getConfig().getInt("api.http-timeout-seconds", 10);
        int maxRetries        = getConfig().getInt("api.http-max-retries", 3);

        if (fallbackNextCheck < 30) {
            getLogger().warning("[PloudStore] fallback-next-check-seconds < 30 — using 30.");
            fallbackNextCheck = 30;
        }

        getLogger().info("[PloudStore] base-url=" + baseUrl
                + " fallback-next-check=" + fallbackNextCheck + "s"
                + " timeout=" + timeout + "s retries=" + maxRetries);

        apiClient = new ApiClient(baseUrl, secretKey, timeout, maxRetries, getLogger());
        commandProcessor = new CommandProcessor(this, apiClient, executedCache, fallbackNextCheck);
        commandProcessor.startChecks();

        evictTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this, executedCache::evictExpired, 72000L, 72000L);

        return true;
    }
}
