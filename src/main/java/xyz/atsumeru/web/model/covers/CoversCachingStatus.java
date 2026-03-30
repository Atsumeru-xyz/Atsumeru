package xyz.atsumeru.web.model.covers;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CoversCachingStatus {
    @Schema(name = "covers_caching_active")
    @SerializedName("covers_caching_active")
    private boolean isCoversCachingActive;

    @Schema(name = "running_ms")
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
