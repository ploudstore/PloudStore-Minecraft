package org.ploudstore.ploudStorePlugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.ploudstore.ploudStorePlugin.api.ApiClient;
import org.ploudstore.ploudStorePlugin.command.VelocityAdminCommand;
import org.ploudstore.ploudStorePlugin.dispatch.VelocityCommandDispatcher;
import org.ploudstore.ploudStorePlugin.executor.CommandProcessor;
import org.ploudstore.ploudStorePlugin.listener.VelocityPlayerJoinListener;
import org.ploudstore.ploudStorePlugin.platform.VelocityPlayerRegistry;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;
import org.ploudstore.ploudStorePlugin.scheduler.ScheduledTask;
import org.ploudstore.ploudStorePlugin.scheduler.VelocitySchedulerAdapter;
import org.ploudstore.ploudStorePlugin.updater.UpdateChecker;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PloudStoreVelocityPlugin {

    private static final String API_BASE_URL            = "https://command.ploudstore.com";
    private static final int    API_FALLBACK_NEXT_CHECK = 60;
    private static final int    API_TIMEOUT_SECONDS     = 10;
    private static final int    API_MAX_RETRIES         = 3;

    private final ProxyServer proxy;
    private final Logger      slf4jLogger;
    private final Path        dataDirectory;

    private Properties        config;
    private ExecutedCache     executedCache;
    private CommandProcessor  commandProcessor;
    private ApiClient         apiClient;
    private ScheduledTask     evictTask;
    private UpdateChecker     updateChecker;
    private PluginLogger      pluginLogger;

    @Inject
    public PloudStoreVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy         = proxy;
        this.slf4jLogger   = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        loadConfig();

        executedCache = new ExecutedCache();
        pluginLogger  = new PluginLogger(slf4jLogger, Boolean.parseBoolean(config.getProperty("debug", "false")));

        String version = readVersion();
        updateChecker = new UpdateChecker(version, pluginLogger);
        updateChecker.checkAsync();

        if (!startCommandProcessor()) return;

        proxy.getEventManager().register(this, new VelocityPlayerJoinListener(this));
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("ploudstore").aliases("plstore-v").build(),
                new VelocityAdminCommand(this, executedCache));

        slf4jLogger.info("[PloudStore] Velocity plugin enabled.");
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); }
        slf4jLogger.info("[PloudStore] Velocity plugin disabled.");
    }

    public ProxyServer     getProxy()           { return proxy; }
    public CommandProcessor getCommandProcessor() { return commandProcessor; }
    public ApiClient        getApiClient()        { return apiClient; }
    public UpdateChecker    getUpdateChecker()    { return updateChecker; }
    public PluginLogger     getPluginLogger()     { return pluginLogger; }

    public void reload() {
        if (evictTask != null)        { evictTask.cancel();      evictTask = null; }
        if (commandProcessor != null) { commandProcessor.stop(); commandProcessor = null; }
        loadConfig();
        pluginLogger.setDebug(Boolean.parseBoolean(config.getProperty("debug", "false")));
        startCommandProcessor();
        slf4jLogger.info("[PloudStore] Reloaded.");
    }

    private boolean startCommandProcessor() {
        String secretKey = config.getProperty("secret-key", "").trim();
        if (secretKey.isEmpty() || secretKey.equals("your-secret-key-here")) {
            slf4jLogger.error("[PloudStore] secret-key is not configured in config.properties!");
            return false;
        }

        VelocitySchedulerAdapter scheduler = new VelocitySchedulerAdapter(this, proxy);

        apiClient        = new ApiClient(API_BASE_URL, secretKey, API_TIMEOUT_SECONDS, API_MAX_RETRIES, pluginLogger);
        commandProcessor = new CommandProcessor(
                apiClient, executedCache, API_FALLBACK_NEXT_CHECK, pluginLogger,
                scheduler, new VelocityCommandDispatcher(proxy),
                new VelocityPlayerRegistry(proxy),
                "&c[Loja] Sem inventário disponível.");
        commandProcessor.startChecks();

        final ExecutedCache cache = executedCache;
        evictTask = scheduler.runAsyncTimer(new Runnable() {
            public void run() { cache.evictExpired(); }
        }, 72000L, 72000L);

        return true;
    }

    private void loadConfig() {
        File dir = dataDirectory.toFile();
        if (!dir.exists()) dir.mkdirs();

        File configFile = new File(dir, "config.properties");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/velocity-config.properties")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                slf4jLogger.warn("[PloudStore] Could not create default config: " + e.getMessage());
            }
        }

        config = new Properties();
        try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
            config.load(reader);
        } catch (IOException e) {
            slf4jLogger.error("[PloudStore] Failed to load config: " + e.getMessage());
        }
    }

    private String readVersion() {
        try (InputStream in = getClass().getResourceAsStream("/velocity-plugin.json")) {
            if (in == null) return "unknown";
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
            String json = buf.toString("UTF-8");
            int idx = json.indexOf("\"version\"");
            if (idx < 0) return "unknown";
            int start = json.indexOf('"', idx + 9) + 1;
            int end   = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
