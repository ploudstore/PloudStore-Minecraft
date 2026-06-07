package org.ploudstore.ploudStorePlugin.queue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory dedup guard: tracks recently executed command IDs.
 * No file persistence — the API is the source of truth.
 * If the server restarts, the API will re-deliver unconfirmed commands.
 */
public class ExecutedCache {

    private static final long TTL_MS = 2 * 60 * 60 * 1000L; // 2 hours

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public boolean contains(String id) {
        Long ts = cache.get(id);
        if (ts == null) return false;
        if ((System.currentTimeMillis() - ts) > TTL_MS) {
            cache.remove(id);
            return false;
        }
        return true;
    }

    public void add(String id) {
        cache.put(id, System.currentTimeMillis());
    }

    public void evictExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> (now - e.getValue()) > TTL_MS);
    }

    public int size() {
        return cache.size();
    }
}
