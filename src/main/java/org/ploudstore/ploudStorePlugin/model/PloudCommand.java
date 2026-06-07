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

    /** Minimum free inventory slots required before executing (0 = no requirement). */
    private int requiredSlots;

    public String getId()              { return id; }
    public String getStatus()          { return status; }
    public String getResolvedCommand() { return resolvedCommand; }
    public String getIdentifier()      { return identifier; }
    public int    getAttempts()        { return attempts; }
    public String getCreatedAt()       { return createdAt; }
    public int    getDelay()           { return delay; }
    public int    getRequiredSlots()   { return requiredSlots; }

    @Override
    public String toString() {
        return "PloudCommand{id='" + id + "', identifier='" + identifier
                + "', resolvedCommand='" + resolvedCommand + "'}";
    }
}
