package xyz.atsumeru.web.model.metadata;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MetadataUpdateStatus {
    @Schema(name = "metadata_update_active")
    @SerializedName("metadata_update_active")
    private boolean isUpdateActive;

    @Schema(name = "running_ms")
    @SerializedName("running_ms")
    private long runningMs;

    private int updated;
    private int total;

    private float percent;

    public MetadataUpdateStatus(boolean isUpdateActive, long runningMs, int updated, int total) {
        this.isUpdateActive = isUpdateActive;
        this.runningMs = runningMs;
        this.updated = updated;
        this.total = total;
        percent = total > 0 ? (float)updated / total * 100 : 0;
    }
}