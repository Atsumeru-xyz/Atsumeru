package com.atsumeru.web.properties;

import com.atsumeru.web.configuration.FileWatcherConfig;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.model.importer.FolderProperty;
import com.atsumeru.web.util.GUFile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FoldersProperties {
    private static final Logger logger = LoggerFactory.getLogger(FoldersProperties.class.getSimpleName());
    private static final String FOLDERS_PROPERTIES_FILENAME = "./folders.properties";

    @Getter private static List<FolderProperty> folderProperties;

    public static void loadFolderPropertiesFile() {
        logger.info("Loading folder properties file...");
        try (JsonReader reader = new JsonReader(new FileReader(FOLDERS_PROPERTIES_FILENAME))) {
            final Type folderType = new TypeToken<List<FolderProperty>>() {}.getType();
            folderProperties = new Gson().fromJson(reader, folderType);
            logger.info("Folder properties file loaded successfully");
        } catch (IOException e) {
            folderProperties = new ArrayList<>();
            logger.warn("Unable to load folder properties...");
        }
    }

    public static void addFolder(FolderProperty folderProperty) {
        folderProperties.add(folderProperty);
        saveProperties();
    }

    public static void removeFolder(FolderProperty folderProperty) {
        folderProperties = folderProperties.stream()
                .filter(property -> !GUString.equalsIgnoreCase(property.getHash(), folderProperty.getHash()))
                .collect(Collectors.toList());

        saveProperties();
    }

    public static boolean containsFolder(String path) {
        String fixedPath = GUFile.addPathSlash(path);
        return folderProperties.stream()
                .map(property -> GUFile.addPathSlash(property.getPath()))
                .anyMatch(propertyPath -> GUString.equalsIgnoreCase(propertyPath, fixedPath));
    }

    private static void saveProperties() {
        GUFile.writeStringToFile(FOLDERS_PROPERTIES_FILENAME, new Gson().toJson(folderProperties));
        FileWatcherConfig.start();
    }

    static {
        loadFolderPropertiesFile();
    }
}
