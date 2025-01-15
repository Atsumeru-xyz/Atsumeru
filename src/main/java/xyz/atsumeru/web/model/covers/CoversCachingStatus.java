package xyz.atsumeru.web.model.covers;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CoversCachingStatus {
    @SerializedName("covers_caching_active")
    private boolean isCoversCachingActive;

    @SerializedName("running_ms")
    private long runningMs;

    private int saved;
    private int total;
    private float percent;

    public CoversCachingStatus(boolean isCoversCachingActive, long runningMs, int saved, int total) {
        this.isCoversCachingActive = isCoversCachingActive;
        this.runningMs = runningMs;
        this.saved = saved;
        this.total = total;
        percent = total > 0 ? (float)saved / total * 100 : 0;
    }
}
