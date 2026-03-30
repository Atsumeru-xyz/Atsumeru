package xyz.atsumeru.web.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ServerInfo {
    private String name;

    private String version;

    @Schema(name = "version_name")
    @SerializedName("version_name")
    private String versionName;

    @Schema(name = "has_password")
    @SerializedName("has_password")
    private boolean hasPassword;

    @Schema(name = "debug_mode")
    @SerializedName("debug_mode")
    private boolean debugMode;

    private Stats stats;

    @Data
    @Accessors(chain = true)
    public static class Stats {
        @Schema(name = "total_series")
        @SerializedName("total_series")
        private long totalSeries;

        @Schema(name = "total_archives")
        @SerializedName("total_archives")
        private long totalArchives;

        @Schema(name = "total_chapters")
        @SerializedName("total_chapters")
        private long totalChapters;

        @Schema(name = "total_categories")
        @SerializedName("total_categories")
        private long totalCategories;
    }
}
