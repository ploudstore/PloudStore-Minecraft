package org.ploudstore.ploudStorePlugin.scheduler;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class BungeeSchedulerAdapter implements PlatformScheduler {

    private final Plugin plugin;

    public BungeeSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getProxy().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        long millis = delayTicks * 50L;
        plugin.getProxy().getScheduler().schedule(plugin, task, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public org.ploudstore.ploudStorePlugin.scheduler.ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        long delayMillis  = delayTicks  * 50L;
        long periodMillis = periodTicks * 50L;
        final ScheduledTask t = plugin.getProxy().getScheduler()
                .schedule(plugin, task, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        return new org.ploudstore.ploudStorePlugin.scheduler.ScheduledTask() {
            public void cancel() { t.cancel(); }
        };
    }

    @Override
    public void runSync(Runnable task) {
        // BungeeCord has no main thread concept — just run async
        runAsync(task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        runAsyncLater(task, delayTicks);
    }
}
