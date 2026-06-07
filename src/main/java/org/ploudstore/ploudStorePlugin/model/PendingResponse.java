package org.ploudstore.ploudStorePlugin.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class PendingResponse {
    @SerializedName("command")
    private List<PloudCommand> data;

    @SerializedName("next_check")
    private int nextCheck;

    @SerializedName("execute_offline")
    private boolean executeOffline;

    public List<PloudCommand> getData() {
        return data != null ? data : Collections.emptyList();
    }

    /** Seconds until the next check. 0 means the plugin should use its fallback interval. */
    public int getNextCheck() {
        return nextCheck;
    }

    /** If true, execute commands even when the player is offline. */
    public boolean isExecuteOffline() {
        return executeOffline;
    }
}
