package org.ploudstore.ploudStorePlugin.model;

import com.google.gson.annotations.SerializedName;

public class CommandConfig {

    @SerializedName("min_slot")
    private int minSlot;

    @SerializedName("required_online")
    private boolean requiredOnline;

    public int getMinSlot()          { return minSlot; }
    public boolean isRequiredOnline() { return requiredOnline; }
}
