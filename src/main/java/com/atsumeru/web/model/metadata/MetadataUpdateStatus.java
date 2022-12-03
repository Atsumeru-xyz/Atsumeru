package com.atsumeru.web.model.metadata;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MetadataUpdateStatus {
    @Expose
    @SerializedName("metadata_update_active")
    private boolean isUpdateActive;

    @Expose
    @SerializedName("running_ms")
    private long runningMs;

    @Expose
    private int updated;

    @Expose
    private int total;

    @Expose
    private float percent;

    public MetadataUpdateStatus(boolean isUpdateActive, long runningMs, int updated, int total) {
        this.isUpdateActive = isUpdateActive;
        this.runningMs = runningMs;
        this.updated = updated;
        this.total = total;
        percent = total > 0 ? (float)updated / total * 100 : 0;
    }
}