package org.ploudstore.ploudStorePlugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.ploudstore.ploudStorePlugin.api.ApiClient;
import org.ploudstore.ploudStorePlugin.command.PloudStoreCommand;
import org.ploudstore.ploudStorePlugin.dispatch.BukkitCommandDispatcher;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.listener.PlayerJoinListener;
import org.ploudstore.ploudStorePlugin.platform.BukkitPlayerRegistry;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;
import org.ploudstore.ploudStorePlugin.scheduler.BukkitSchedulerAdapter;
import org.ploudstore.ploudStorePlugin.scheduler.FoliaSchedulerAdapter;
import org.ploudstore.ploudStorePlugin.scheduler.PlatformScheduler;
import org.ploudstore.ploudStorePlugin.scheduler.ScheduledTask;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;

public final class PloudStorePlugin extends JavaPlugin {

    private static final String API_BASE_URL            = "https://command.ploudstore.com";
    private static final int    API_FALLBACK_NEXT_CHECK = 60;
    private static final int    API_TIMEOUT_SECONDS     = 10;
    private static final int    API_MAX_RETRIES         = 3;

    private ExecutedCache     executedCache;
    private CommandProcessor  commandProcessor;
    private ApiClient         apiClient;
    private ScheduledTask     evictTask;
    private UpdateChecker     updateChecker;
    private PluginLogger      pluginLogger;
    private PlatformScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        executedCache = new ExecutedCache();
        pluginLogger  = new PluginLogger(getLogger(), getConfig().getBoolean("debug", false));
        scheduler     = isFolia() ? new FoliaSchedulerAdapter(this) : new BukkitSchedulerAdapter(this);

        if (isFolia()) {
            getLogger().info("[PloudStore] Folia detectado — usando Folia schedulers.");
        }

        updateChecker = new UpdateChecker(getDescription().getVersion(), pluginLogger);
        updateChecker.checkAsync();

        if (!startCommandProcessor()) return;

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        PloudStoreCommand ploudStoreCmd = new PloudStoreCommand(this, executedCache);
        PluginCommand cmd = getCommand("ploudstore");
        if (cmd != null) {
            cmd.setExecutor(ploudStoreCmd);
            cmd.setTabCompleter(ploudStoreCmd);
        }

        getLogger().info("[PloudStore] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); }
        getLogger().info("[PloudStore] Plugin disabled.");
    }

    public CommandProcessor getCommandProcessor() { return commandProcessor; }
    public ApiClient        getApiClient()        { return apiClient; }
    public UpdateChecker    getUpdateChecker()    { return updateChecker; }
    public PluginLogger     getPluginLogger()     { return pluginLogger; }
    public PlatformScheduler getPluginScheduler() { return scheduler; }

    public void reload() {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); commandProcessor = null; }
        reloadConfig();
        pluginLogger.setDebug(getConfig().getBoolean("debug", false));
        startCommandProcessor();
        getLogger().info("[PloudStore] Reloaded.");
    }

    private boolean startCommandProcessor() {
        String secretKey = getConfig().getString("secret-key", "").trim();
        if (secretKey.isEmpty() || secretKey.equals("your-secret-key-here")) {
            getLogger().severe("[PloudStore] secret-key is not configured in config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        String notEnoughSlotsMsg = getConfig().getString(
                "messages.not-enough-slots",
                "&c[Loja] Precisa de {slots} slot(s) livre(s) no inventario para receber a sua compra. Tem apenas {free}.");

        apiClient        = new ApiClient(API_BASE_URL, secretKey, API_TIMEOUT_SECONDS, API_MAX_RETRIES, pluginLogger);
        commandProcessor = new CommandProcessor(
                apiClient, executedCache, API_FALLBACK_NEXT_CHECK, pluginLogger,
                scheduler, new BukkitCommandDispatcher(), new BukkitPlayerRegistry(),
                notEnoughSlotsMsg);
        commandProcessor.startChecks();

        final ExecutedCache cache = executedCache;
        evictTask = scheduler.runAsyncTimer(new Runnable() {
            public void run() { cache.evictExpired(); }
        }, 72000L, 72000L);

        return true;
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
