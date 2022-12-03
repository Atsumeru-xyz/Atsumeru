package com.atsumeru.web.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ServerInfo {
    @Expose
    private String name;

    @Expose
    private String version;

    @Expose
    @SerializedName("version_name")
    private String versionName;

    @Expose
    @SerializedName("has_password")
    private boolean hasPassword;

    @Expose
    @SerializedName("debug_mode")
    private boolean debugMode;

    @Expose
    private Stats stats;

    @Data
    @Accessors(chain = true)
    public static class Stats {
        @Expose
        @SerializedName("total_series")
        private long totalSeries;

        @Expose
        @SerializedName("total_archives")
        private long totalArchives;

        @Expose
        @SerializedName("total_chapters")
        private long totalChapters;

        @Expose
        @SerializedName("total_categories")
        private long totalCategories;
    }
}
