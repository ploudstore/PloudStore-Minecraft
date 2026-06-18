package org.ploudstore.ploudStorePlugin.platform;

public interface PlayerRegistry {
    boolean isOnline(String name);
    int getFreeSlots(String name);
    void sendMessage(String name, String message);
}
