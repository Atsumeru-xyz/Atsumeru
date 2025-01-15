package xyz.atsumeru.web.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.TypeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

@Service
@DependsOn("workspace")
public class Settings {
    private static final Logger logger = LoggerFactory.getLogger(Settings.class.getSimpleName());

    private static Properties properties;
    private static final String PROPERTIES_FILENAME = "atsumeru.properties";

    public Settings() {
        properties = new Properties();
        FileInputStream fis = null;
        try {
            properties.load(fis = new FileInputStream(new File(Workspace.CONFIG_DIR, PROPERTIES_FILENAME)));
            logger.info("Settings from " + PROPERTIES_FILENAME + " loaded successfully!");
        } catch (IOException e) {
            logger.warn("Unable to load " + PROPERTIES_FILENAME + ". A new one will be created...");
        } finally {
            FileUtils.closeLoudly(fis);
        }
    }

    private static void setProperty(String propertyName, String propertyValue) {
        properties.setProperty(propertyName, propertyValue);
        saveProperties();
    }

    private static void saveProperties() {
        try (FileOutputStream outputStream = new FileOutputStream(new File(Workspace.CONFIG_DIR, PROPERTIES_FILENAME))) {
            properties.store(outputStream, "Auto save properties: " + new Date());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Unable to save " + PROPERTIES_FILENAME);
        }
    }

    public static final String KEY_ALLOW_LOADING_LIST_WITH_VOLUMES = "allow_loading_list_with_volumes";
    private static final boolean DEFAULT_ALLOW_LOADING_LIST_WITH_VOLUMES = true;

    public static boolean isAllowListLoadingWithVolumes() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_ALLOW_LOADING_LIST_WITH_VOLUMES), DEFAULT_ALLOW_LOADING_LIST_WITH_VOLUMES);
    }

    public static void putAllowListLoadingWithVolumes(boolean value) {
        setProperty(KEY_ALLOW_LOADING_LIST_WITH_VOLUMES, String.valueOf(value));
    }

    public static final String KEY_ALLOW_LOADING_LIST_WITH_CHAPTERS = "allow_loading_list_with_chapters";
    private static final boolean DEFAULT_ALLOW_LOADING_LIST_WITH_CHAPTERS = true;

    public static boolean isAllowListLoadingWithChapters() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_ALLOW_LOADING_LIST_WITH_CHAPTERS), DEFAULT_ALLOW_LOADING_LIST_WITH_CHAPTERS);
    }

    public static void putAllowListLoadingWithChapters(boolean value) {
        setProperty(KEY_ALLOW_LOADING_LIST_WITH_CHAPTERS, String.valueOf(value));
    }

    public static final String KEY_DISABLE_REQUEST_LOGGING_INTO_CONSOLE = "disable_request_logging_into_console";
    private static final boolean DEFAULT_DISABLE_REQUEST_LOGGING_INTO_CONSOLE = false;

    public static boolean isDisableRequestLoggingIntoConsole() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_DISABLE_REQUEST_LOGGING_INTO_CONSOLE), DEFAULT_DISABLE_REQUEST_LOGGING_INTO_CONSOLE);
    }

    public static void putDisableRequestLoggingIntoConsole(boolean value) {
        setProperty(KEY_DISABLE_REQUEST_LOGGING_INTO_CONSOLE, String.valueOf(value));
    }

    public static final String KEY_DISABLE_FILE_WATCHER = "disable_file_watcher";
    private static final boolean DEFAULT_DISABLE_FILE_WATCHER = false;

    public static boolean isDisableFileWatcher() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_DISABLE_FILE_WATCHER), DEFAULT_DISABLE_FILE_WATCHER);
    }

    public static void putDisableFileWatcher(boolean value) {
        setProperty(KEY_DISABLE_FILE_WATCHER, String.valueOf(value));
    }

    public static final String KEY_DISABLE_WATCH_FOR_MODIFIED_FILES = "disable_watch_for_modified_files";
    private static final boolean DEFAULT_DISABLE_WATCH_FOR_MODIFIED_FILES = false;

    public static boolean isDisableWatchForModifiedFiles() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_DISABLE_WATCH_FOR_MODIFIED_FILES), DEFAULT_DISABLE_WATCH_FOR_MODIFIED_FILES);
    }

    public static void putDisableWatchForModifiedFiles(boolean value) {
        setProperty(KEY_DISABLE_WATCH_FOR_MODIFIED_FILES, String.valueOf(value));
    }

    public static final String KEY_DISABLE_CHAPTERS = "disable_chapters";
    private static final boolean DEFAULT_DISABLE_CHAPTERS = true;

    public static boolean isDisableChapters() {
        return TypeUtils.getBoolDef(properties.getProperty(KEY_DISABLE_CHAPTERS), DEFAULT_DISABLE_CHAPTERS);
    }

    public static void putDisableChapters(boolean value) {
        setProperty(KEY_DISABLE_CHAPTERS, String.valueOf(value));
    }
}
