package com.atsumeru.web.model.importer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ImportStatus {
    @Expose
    @SerializedName("import_active")
    private boolean isActive;

    @Expose
    @SerializedName("last_start_time")
    private long lastStartTime;

    @Expose
    @SerializedName("running_ms")
    private long runningMs;

    @Expose
    private int imported;

    @Expose
    private int total;

    @Expose
    private float percent;

    public ImportStatus(boolean isActive, long lastStartTime, long runningMs, int imported, int total) {
        this.isActive = isActive;
        this.lastStartTime = lastStartTime;
        this.runningMs = runningMs;
        this.imported = imported;
        this.total = total;
        percent = total > 0 ? (float)imported / total * 100 : 0;
    }
}
