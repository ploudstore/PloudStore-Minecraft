package org.ploudstore.ploudStorePlugin.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;

public class VelocitySchedulerAdapter implements PlatformScheduler {

    private final Object plugin;
    private final ProxyServer proxy;

    public VelocitySchedulerAdapter(Object plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy  = proxy;
    }

    @Override
    public void runAsync(Runnable task) {
        proxy.getScheduler()
                .buildTask(plugin, task)
                .schedule();
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        proxy.getScheduler()
                .buildTask(plugin, task)
                .delay(delayTicks * 50L, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        final com.velocitypowered.api.scheduler.ScheduledTask t = proxy.getScheduler()
                .buildTask(plugin, task)
                .delay(delayTicks * 50L, TimeUnit.MILLISECONDS)
                .repeat(periodTicks * 50L, TimeUnit.MILLISECONDS)
                .schedule();
        return new ScheduledTask() {
            public void cancel() { t.cancel(); }
        };
    }

    @Override
    public void runSync(Runnable task) {
        // Velocity has no main thread — run async
        runAsync(task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        runAsyncLater(task, delayTicks);
    }
}
