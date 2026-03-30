package xyz.atsumeru.web.model.importer;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ImportStatus {
    @Schema(name = "import_active")
    @SerializedName("import_active")
    private boolean isActive;

    @Schema(name = "last_start_time")
    @SerializedName("last_start_time")
    private long lastStartTime;

    @Schema(name = "running_ms")
    @SerializedName("running_ms")
    private long runningMs;

    private int imported;
    private int total;
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
