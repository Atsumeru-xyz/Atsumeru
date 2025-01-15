package xyz.atsumeru.web.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ServerInfo {
    private String name;

    private String version;

    @SerializedName("version_name")
    private String versionName;

    @SerializedName("has_password")
    private boolean hasPassword;

    @SerializedName("debug_mode")
    private boolean debugMode;

    private Stats stats;

    @Data
    @Accessors(chain = true)
    public static class Stats {
        @SerializedName("total_series")
        private long totalSeries;

        @SerializedName("total_archives")
        private long totalArchives;

        @SerializedName("total_chapters")
        private long totalChapters;

        @SerializedName("total_categories")
        private long totalCategories;
    }
}
