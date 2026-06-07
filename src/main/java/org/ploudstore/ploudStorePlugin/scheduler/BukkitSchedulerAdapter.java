package org.ploudstore.ploudStorePlugin.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitSchedulerAdapter implements PlatformScheduler {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        final org.bukkit.scheduler.BukkitTask t =
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return new ScheduledTask() {
            public void cancel() { t.cancel(); }
        };
    }

    @Override
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
