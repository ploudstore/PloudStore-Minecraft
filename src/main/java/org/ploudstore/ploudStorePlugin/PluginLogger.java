package org.ploudstore.ploudStorePlugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class PluginLogger {

    private final Consumer<String> infoFn;
    private final Consumer<String> warnFn;
    private final Consumer<String> errorFn;
    private volatile boolean debug;

    public PluginLogger(Logger logger, boolean debug) {
        this.infoFn  = logger::info;
        this.warnFn  = logger::warning;
        this.errorFn = logger::severe;
        this.debug   = debug;
    }

    public PluginLogger(org.slf4j.Logger logger, boolean debug) {
        this.infoFn  = logger::info;
        this.warnFn  = logger::warn;
        this.errorFn = logger::error;
        this.debug   = debug;
    }

    public void setDebug(boolean debug) { this.debug = debug; }

    public void debug(String msg)   { if (debug) infoFn.accept("[DEBUG] " + msg); }
    public void info(String msg)    { infoFn.accept(msg); }
    public void warning(String msg) { warnFn.accept(msg); }
    public void severe(String msg)  { errorFn.accept(msg); }
}
