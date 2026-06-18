package org.ploudstore.ploudStorePlugin.model;

public class PloudCommand {

    @com.google.gson.annotations.SerializedName("command_id")
    private String id;

    private String status;

    @com.google.gson.annotations.SerializedName("command")
    private String resolvedCommand;

    @com.google.gson.annotations.SerializedName("username")
    private String identifier;
    private int attempts;
    private String createdAt;
    private String updatedAt;
    private String lastAttemptAt;
    private String executedAt;
    private String errorMessage;
    private String paymentItemId;

    /** Seconds to wait before executing this command (0 = execute immediately). */
    private int delay;

    /** Raw JSON string from the API, e.g. {"min_slot":1,"required_online":true} */
    @com.google.gson.annotations.SerializedName("config")
    private String configJson;

    private transient CommandConfig parsedConfig;

    private CommandConfig config() {
        if (parsedConfig == null) {
            if (configJson != null && !configJson.isEmpty()) {
                try {
                    parsedConfig = new com.google.gson.Gson().fromJson(configJson, CommandConfig.class);
                } catch (Exception ignored) {}
            }
            if (parsedConfig == null) parsedConfig = new CommandConfig();
        }
        return parsedConfig;
    }

    public String getId()              { return id; }
    public String getStatus()          { return status; }
    public String getResolvedCommand() { return resolvedCommand; }
    public String getIdentifier()      { return identifier; }
    public int    getAttempts()        { return attempts; }
    public String getCreatedAt()       { return createdAt; }
    public int    getDelay()           { return delay; }
    public int    getRequiredSlots()   { return config().getMinSlot(); }
    public boolean isRequiredOnline()  { return config().isRequiredOnline(); }

    @Override
    public String toString() {
        return "PloudCommand{id='" + id + "', identifier='" + identifier
                + "', resolvedCommand='" + resolvedCommand + "'}";
    }
}
