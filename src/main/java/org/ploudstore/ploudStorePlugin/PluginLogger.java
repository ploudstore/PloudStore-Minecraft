package org.ploudstore.ploudStorePlugin;

import java.util.logging.Logger;

public class PluginLogger {

    private final Logger logger;
    private volatile boolean debug;

    public PluginLogger(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void debug(String msg) {
        if (debug) logger.info("[DEBUG] " + msg);
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void warning(String msg) {
        logger.warning(msg);
    }

    public void severe(String msg) {
        logger.severe(msg);
    }
}
