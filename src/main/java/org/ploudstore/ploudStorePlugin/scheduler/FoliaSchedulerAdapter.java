package org.ploudstore.ploudStorePlugin.scheduler;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaSchedulerAdapter implements PlatformScheduler {

    private final Plugin plugin;
    private final Object asyncScheduler;
    private final Object globalScheduler;

    private final Method asyncRunNow;
    private final Method asyncRunDelayed;
    private final Method asyncRunAtFixedRate;
    private final Method globalRun;
    private final Method globalRunDelayed;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        try {
            Object server = plugin.getServer();
            this.asyncScheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
            this.globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);

            Class<?> asyncClass = asyncScheduler.getClass();
            Class<?> globalClass = globalScheduler.getClass();

            this.asyncRunNow       = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
            this.asyncRunDelayed   = asyncClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
            this.asyncRunAtFixedRate = asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
            this.globalRun         = globalClass.getMethod("run", Plugin.class, Consumer.class);
            this.globalRunDelayed  = globalClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
        } catch (Exception e) {
            throw new RuntimeException("[PloudStore] Failed to initialise Folia schedulers", e);
        }
    }

    @Override
    public void runAsync(final Runnable task) {
        invoke(asyncRunNow, asyncScheduler, plugin, wrap(task));
    }

    @Override
    public void runAsyncLater(final Runnable task, long delayTicks) {
        long ms = Math.max(1L, delayTicks * 50L);
        invoke(asyncRunDelayed, asyncScheduler, plugin, wrap(task), ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledTask runAsyncTimer(final Runnable task, long delayTicks, long periodTicks) {
        long delayMs  = Math.max(1L, delayTicks * 50L);
        long periodMs = Math.max(1L, periodTicks * 50L);
        final Object foliaTask = invoke(asyncRunAtFixedRate, asyncScheduler,
                plugin, wrap(task), delayMs, periodMs, TimeUnit.MILLISECONDS);
        return new ScheduledTask() {
            public void cancel() {
                try {
                    foliaTask.getClass().getMethod("cancel").invoke(foliaTask);
                } catch (Exception ignored) {}
            }
        };
    }

    @Override
    public void runSync(final Runnable task) {
        invoke(globalRun, globalScheduler, plugin, wrap(task));
    }

    @Override
    public void runSyncLater(final Runnable task, long delayTicks) {
        invoke(globalRunDelayed, globalScheduler, plugin, wrap(task), delayTicks);
    }

    private Consumer<Object> wrap(final Runnable task) {
        return new Consumer<Object>() {
            public void accept(Object t) { task.run(); }
        };
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("[PloudStore] Folia scheduler call failed: " + method.getName(), e);
        }
    }
}
