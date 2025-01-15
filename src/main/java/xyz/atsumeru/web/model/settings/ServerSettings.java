package xyz.atsumeru.web.model.settings;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.atsumeru.web.manager.Settings;

@Data
@AllArgsConstructor
public class ServerSettings {
    @SerializedName(Settings.KEY_ALLOW_LOADING_LIST_WITH_VOLUMES)
    private boolean allowLoadingListWithVolumes;

    @SerializedName(Settings.KEY_ALLOW_LOADING_LIST_WITH_CHAPTERS)
    private boolean allowLoadingListWithChapters;

    @SerializedName(Settings.KEY_DISABLE_REQUEST_LOGGING_INTO_CONSOLE)
    private boolean isDisableRequestLoggingIntoConsole;

    @SerializedName(Settings.KEY_DISABLE_FILE_WATCHER)
    private boolean disableFileWatcher;

    @SerializedName(Settings.KEY_DISABLE_WATCH_FOR_MODIFIED_FILES)
    private boolean disableWatchForModifiedFiles;

    @SerializedName(Settings.KEY_DISABLE_CHAPTERS)
    private boolean disableChapters;
}
