package org.ploudstore.ploudStorePlugin;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.ploudstore.ploudStorePlugin.api.ApiClient;
import org.ploudstore.ploudStorePlugin.command.BungeeAdminCommand;
import org.ploudstore.ploudStorePlugin.dispatch.BungeeCommandDispatcher;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.listener.BungeePlayerJoinListener;
import org.ploudstore.ploudStorePlugin.platform.BungeePlayerRegistry;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;
import org.ploudstore.ploudStorePlugin.scheduler.BungeeSchedulerAdapter;
import org.ploudstore.ploudStorePlugin.scheduler.ScheduledTask;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class PloudStoreBungeePlugin extends Plugin {

    private static final String API_BASE_URL            = "https://command.ploudstore.com";
    private static final int    API_FALLBACK_NEXT_CHECK = 60;
    private static final int    API_TIMEOUT_SECONDS     = 10;
    private static final int    API_MAX_RETRIES         = 3;

    private Configuration    config;
    private ExecutedCache    executedCache;
    private CommandProcessor commandProcessor;
    private ApiClient        apiClient;
    private ScheduledTask    evictTask;
    private UpdateChecker    updateChecker;
    private PluginLogger     pluginLogger;

    @Override
    public void onEnable() {
        loadConfig();

        executedCache = new ExecutedCache();
        pluginLogger  = new PluginLogger(getLogger(), config.getBoolean("debug", false));

        updateChecker = new UpdateChecker(getDescription().getVersion(), pluginLogger);
        updateChecker.checkAsync();

        if (!startCommandProcessor()) return;

        getProxy().getPluginManager().registerListener(this, new BungeePlayerJoinListener(this));
        getProxy().getPluginManager().registerCommand(this, new BungeeAdminCommand(this, executedCache));

        getLogger().info("[PloudStore] BungeeCord plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); }
        getLogger().info("[PloudStore] BungeeCord plugin disabled.");
    }

    public CommandProcessor getCommandProcessor() { return commandProcessor; }
    public ApiClient        getApiClient()        { return apiClient; }
    public UpdateChecker    getUpdateChecker()    { return updateChecker; }
    public PluginLogger     getPluginLogger()     { return pluginLogger; }

    public void reload() {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); commandProcessor = null; }
        loadConfig();
        pluginLogger.setDebug(config.getBoolean("debug", false));
        startCommandProcessor();
        getLogger().info("[PloudStore] Reloaded.");
    }

    private boolean startCommandProcessor() {
        String secretKey = config.getString("secret-key", "").trim();
        if (secretKey.isEmpty() || secretKey.equals("your-secret-key-here")) {
            getLogger().severe("[PloudStore] secret-key is not configured in config.yml!");
            return false;
        }

        BungeeSchedulerAdapter scheduler = new BungeeSchedulerAdapter(this);

        apiClient        = new ApiClient(API_BASE_URL, secretKey, API_TIMEOUT_SECONDS, API_MAX_RETRIES, pluginLogger);
        commandProcessor = new CommandProcessor(
                apiClient, executedCache, API_FALLBACK_NEXT_CHECK, pluginLogger,
                scheduler, new BungeeCommandDispatcher(getProxy()),
                new BungeePlayerRegistry(getProxy()),
                "&c[Loja] Sem inventário disponível."); // slots not applicable on BungeeCord
        commandProcessor.startChecks();

        final ExecutedCache cache = executedCache;
        evictTask = scheduler.runAsyncTimer(new Runnable() {
            public void run() { cache.evictExpired(); }
        }, 72000L, 72000L);

        return true;
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/bungee-config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                getLogger().warning("[PloudStore] Could not create default config: " + e.getMessage());
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("[PloudStore] Failed to load config: " + e.getMessage());
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new java.io.StringReader("secret-key: \"your-secret-key-here\"\ndebug: false\n"));
        }
    }
}
