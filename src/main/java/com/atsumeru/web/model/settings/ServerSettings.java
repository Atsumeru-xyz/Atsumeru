package com.atsumeru.web.model.settings;

import com.atsumeru.web.manager.Settings;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerSettings {
    @Expose
    @SerializedName(Settings.KEY_ALLOW_LOADING_LIST_WITH_VOLUMES)
    private boolean allowLoadingListWithVolumes;

    @Expose
    @SerializedName(Settings.KEY_ALLOW_LOADING_LIST_WITH_CHAPTERS)
    private boolean allowLoadingListWithChapters;

    @Expose
    @SerializedName(Settings.KEY_DISABLE_REQUEST_LOGGING_INTO_CONSOLE)
    private boolean isDisableRequestLoggingIntoConsole;

    @Expose
    @SerializedName(Settings.KEY_DISABLE_BONJOUR_SERVICE)
    private boolean disableBonjourService;

    @Expose
    @SerializedName(Settings.KEY_DISABLE_FILE_WATCHER)
    private boolean disableFileWatcher;

    @Expose
    @SerializedName(Settings.KEY_DISABLE_WATCH_FOR_MODIFIED_FILES)
    private boolean disableWatchForModifiedFiles;

    @Expose
    @SerializedName(Settings.KEY_DISABLE_CHAPTERS)
    private boolean disableChapters;
}
