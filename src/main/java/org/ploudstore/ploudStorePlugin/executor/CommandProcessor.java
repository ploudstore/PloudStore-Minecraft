package org.ploudstore.ploudStorePlugin.executor;

import org.ploudstore.ploudStorePlugin.PluginLogger;
import org.ploudstore.ploudStorePlugin.api.ApiClient;
import org.ploudstore.ploudStorePlugin.dispatch.CommandDispatcher;
import org.ploudstore.ploudStorePlugin.model.FetchResult;
import org.ploudstore.ploudStorePlugin.model.PendingResponse;
import org.ploudstore.ploudStorePlugin.model.PloudCommand;
import org.ploudstore.ploudStorePlugin.platform.PlayerRegistry;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;
import org.ploudstore.ploudStorePlugin.scheduler.PlatformScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandProcessor {

    private static final int BATCH_SIZE = 3;

    private final ApiClient          apiClient;
    private final ExecutedCache      executedCache;
    private final PluginLogger       logger;
    private final PlatformScheduler  scheduler;
    private final CommandDispatcher  dispatcher;
    private final PlayerRegistry     playerRegistry;
    private final int                fallbackNextCheck;
    private final String             notEnoughSlotsMessage;

    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private volatile boolean stopped = false;

    private final Set<String> queuedPlayers = ConcurrentHashMap.newKeySet();

    public CommandProcessor(ApiClient apiClient, ExecutedCache executedCache,
                            int fallbackNextCheck, PluginLogger logger,
                            PlatformScheduler scheduler, CommandDispatcher dispatcher,
                            PlayerRegistry playerRegistry, String notEnoughSlotsMessage) {
        this.apiClient             = apiClient;
        this.executedCache         = executedCache;
        this.fallbackNextCheck     = fallbackNextCheck;
        this.logger                = logger;
        this.scheduler             = scheduler;
        this.dispatcher            = dispatcher;
        this.playerRegistry        = playerRegistry;
        this.notEnoughSlotsMessage = notEnoughSlotsMessage;
    }

    public void startChecks() {
        scheduler.runAsyncLater(new Runnable() {
            public void run() { performCheck(); }
        }, 100L);
    }

    public void stop() { stopped = true; }

    public void requestCheck() {
        scheduler.runAsync(new Runnable() {
            public void run() { performCheck(); }
        });
    }

    public Set<String> getQueuedPlayers() { return queuedPlayers; }

    public void performCheck() {
        if (stopped) return;
        if (!checkInProgress.compareAndSet(false, true)) {
            logger.debug("[PloudStore] Check already in progress — skipping.");
            return;
        }
        try {
            doCheck();
        } finally {
            checkInProgress.set(false);
        }
    }

    private void scheduleNextCheck(final int seconds) {
        if (stopped) return;
        scheduler.runAsyncLater(new Runnable() {
            public void run() { performCheck(); }
        }, (long) seconds * 20L);
    }

    private void doCheck() {
        logger.debug("[PloudStore] Checking for pending commands...");
        queuedPlayers.clear();

        FetchResult result = apiClient.fetchPendingCommands();

        switch (result.getStatus()) {
            case RATE_LIMITED:
                logger.warning("[PloudStore] Rate limited (429) — retrying in 5 minutes.");
                scheduleNextCheck(5 * 60);
                return;
            case FORBIDDEN:
                logger.warning("[PloudStore] Forbidden (403) — check your secret key. Retrying in 30 minutes.");
                scheduleNextCheck(30 * 60);
                return;
            case ERROR:
                logger.warning("[PloudStore] Fetch failed — retrying in 1 minute.");
                scheduleNextCheck(60);
                return;
            default:
                break;
        }

        PendingResponse response = result.getResponse();
        int nextCheck = response.getNextCheck();
        if (nextCheck <= 0) nextCheck = fallbackNextCheck;
        scheduleNextCheck(nextCheck);

        List<PloudCommand> commands = response.getData();
        if (commands.isEmpty()) {
            logger.debug("[PloudStore] No pending commands.");
            return;
        }

        logger.debug("[PloudStore] Found " + commands.size() + " pending command(s).");
        processCommands(commands);
    }

    private void processCommands(List<PloudCommand> commands) {
        List<String> completedIds = new ArrayList<String>();

        for (PloudCommand cmd : commands) {
            if (!isValid(cmd)) continue;

            if (executedCache.contains(cmd.getId())) {
                logger.debug("[PloudStore] Already confirmed (cache hit): " + cmd.getId());
                completedIds.add(cmd.getId());
                if (completedIds.size() % BATCH_SIZE == 0) {
                    confirmBatch(new ArrayList<String>(completedIds));
                    completedIds.clear();
                }
                continue;
            }

            int requiredSlots  = cmd.getRequiredSlots();
            boolean mustBeOnline = cmd.isRequiredOnline();
            logger.debug(String.format(
                    "[PloudStore] cmd=%s user=%s requiredSlots=%d mustBeOnline=%b",
                    cmd.getId(), cmd.getIdentifier(), requiredSlots, mustBeOnline));

            if (mustBeOnline) {
                if (!playerRegistry.isOnline(cmd.getIdentifier())) {
                    logger.debug("[PloudStore] " + cmd.getIdentifier() + " is offline — skipping.");
                    queuedPlayers.add(cmd.getIdentifier().toLowerCase(Locale.ROOT));
                    continue;
                }

                if (requiredSlots > 0) {
                    int freeSlots = playerRegistry.getFreeSlots(cmd.getIdentifier());
                    logger.debug(String.format(
                            "[PloudStore] Slot check for %s — freeSlots=%d requiredSlots=%d PASS=%b",
                            cmd.getIdentifier(), freeSlots, requiredSlots, freeSlots >= requiredSlots));
                    if (freeSlots < requiredSlots) {
                        logger.debug(String.format(
                                "[PloudStore] BLOCKED '%s' for %s — needs %d slots, only %d free.",
                                cmd.getId(), cmd.getIdentifier(), requiredSlots, freeSlots));
                        final String msg = notEnoughSlotsMessage
                                .replace("{slots}", String.valueOf(requiredSlots))
                                .replace("{free}",  String.valueOf(freeSlots))
                                .replace("&", "§");
                        final String playerName = cmd.getIdentifier();
                        playerRegistry.sendMessage(playerName, msg);
                        continue;
                    }
                }
            }

            final String commandStr = resolveCommand(cmd);
            if (commandStr == null) continue;

            final String finalId = cmd.getId();
            if (cmd.getDelay() > 0) {
                scheduler.runSyncLater(new Runnable() {
                    public void run() { dispatchAndLog(commandStr, finalId); }
                }, (long) cmd.getDelay() * 20L);
            } else {
                scheduler.runSync(new Runnable() {
                    public void run() { dispatchAndLog(commandStr, finalId); }
                });
            }

            executedCache.add(cmd.getId());
            completedIds.add(cmd.getId());

            if (completedIds.size() % BATCH_SIZE == 0) {
                confirmBatch(new ArrayList<String>(completedIds));
                completedIds.clear();
            }
        }

        if (!completedIds.isEmpty()) {
            confirmBatch(completedIds);
        }
    }

    private void dispatchAndLog(String command, String id) {
        try {
            boolean ok = dispatcher.dispatch(command);
            if (ok) {
                logger.debug("[PloudStore] Executed [" + id + "]: " + command);
            } else {
                logger.warning("[PloudStore] Command returned false [" + id + "]: " + command
                        + " — already confirmed. Check command syntax.");
            }
        } catch (Exception e) {
            logger.severe("[PloudStore] Exception executing [" + id + "] '" + command + "': " + e.getMessage()
                    + " — already confirmed. Check command syntax.");
        }
    }

    private void confirmBatch(List<String> ids) {
        scheduleConfirm(new ArrayList<String>(ids), 0);
    }

    private void scheduleConfirm(final List<String> ids, final int attempt) {
        scheduler.runAsync(new Runnable() {
            public void run() {
                if (stopped) return;
                if (apiClient.deleteCommands(ids)) {
                    logger.debug("[PloudStore] Confirmed " + ids.size() + " command(s): " + ids);
                    return;
                }
                final long delaySecs = attempt < 5 ? 10L * (1L << attempt) : 300L;
                logger.warning(String.format(
                        "[PloudStore] Confirm failed (attempt %d) — retrying in %ds: %s",
                        attempt + 1, delaySecs, ids));
                scheduler.runAsyncLater(new Runnable() {
                    public void run() { scheduleConfirm(ids, attempt + 1); }
                }, delaySecs * 20L);
            }
        });
    }

    private String resolveCommand(PloudCommand cmd) {
        String raw = cmd.getResolvedCommand();
        if (raw == null || raw.trim().isEmpty()) {
            logger.warning("[PloudStore] Empty resolvedCommand for ID " + cmd.getId() + " — skipping.");
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private boolean isValid(PloudCommand cmd) {
        if (cmd == null || cmd.getId() == null) {
            logger.warning("[PloudStore] Received null command or null ID — skipping.");
            return false;
        }
        if (cmd.getResolvedCommand() == null || cmd.getResolvedCommand().trim().isEmpty()) {
            logger.warning("[PloudStore] Command " + cmd.getId() + " has empty resolvedCommand — skipping.");
            return false;
        }
        if (cmd.getIdentifier() == null || cmd.getIdentifier().trim().isEmpty()) {
            logger.warning("[PloudStore] Command " + cmd.getId() + " has no player identifier — skipping.");
            return false;
        }
        return true;
    }
}
