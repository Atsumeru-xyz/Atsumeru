package com.atsumeru.web.model.covers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CoversCachingStatus {
    @Expose
    @SerializedName("covers_caching_active")
    private boolean isCoversCachingActive;

    @Expose
    @SerializedName("running_ms")
    private long runningMs;

    @Expose
    private int saved;

    @Expose
    private int total;

    @Expose
    private float percent;

    public CoversCachingStatus(boolean isCoversCachingActive, long runningMs, int saved, int total) {
        this.isCoversCachingActive = isCoversCachingActive;
        this.runningMs = runningMs;
        this.saved = saved;
        this.total = total;
        percent = total > 0 ? (float)saved / total * 100 : 0;
    }
}
