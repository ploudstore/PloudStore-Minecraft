package org.ploudstore.ploudStorePlugin.scheduler;

public interface PlatformScheduler {
    void runAsync(Runnable task);
    void runAsyncLater(Runnable task, long delayTicks);
    ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks);
    void runSync(Runnable task);
    void runSyncLater(Runnable task, long delayTicks);
}
