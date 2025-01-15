package xyz.atsumeru.web.properties;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import xyz.atsumeru.web.configuration.FileWatcherConfiguration;
import xyz.atsumeru.web.manager.Workspace;
import xyz.atsumeru.web.model.importer.ImportFolder;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@DependsOn("workspace")
public class ImportFolders {
    private static final Logger logger = LoggerFactory.getLogger(ImportFolders.class.getSimpleName());
    private static final String FOLDERS_PROPERTIES_FILENAME = "folders.properties";

    @Getter private static List<ImportFolder> folderProperties;

    public ImportFolders() {
        try (JsonReader reader = new JsonReader(new FileReader(new File(Workspace.CONFIG_DIR, FOLDERS_PROPERTIES_FILENAME)))) {
            final Type folderType = new TypeToken<List<ImportFolder>>() {}.getType();
            folderProperties = new Gson().fromJson(reader, folderType);
            logger.info("Import folders from " + FOLDERS_PROPERTIES_FILENAME + " loaded successfully!");
        } catch (IOException e) {
            folderProperties = new ArrayList<>();
            logger.warn("Unable to load " + FOLDERS_PROPERTIES_FILENAME + "... Maybe there is no folders yet?");
        }
    }

    public static void addFolder(ImportFolder importFolder) {
        folderProperties.add(importFolder);
        saveProperties();
    }

    public static void removeFolder(ImportFolder importFolder) {
        folderProperties = folderProperties.stream()
                .filter(property -> !StringUtils.equalsIgnoreCase(property.getHash(), importFolder.getHash()))
                .collect(Collectors.toList());

        saveProperties();
    }

    public static boolean containsFolder(String path) {
        String fixedPath = FileUtils.addPathSlash(path);
        return folderProperties.stream()
                .map(property -> FileUtils.addPathSlash(property.getPath()))
                .anyMatch(propertyPath -> StringUtils.equalsIgnoreCase(propertyPath, fixedPath));
    }

    private static void saveProperties() {
        FileUtils.writeStringToFile(new File(Workspace.CONFIG_DIR, FOLDERS_PROPERTIES_FILENAME), new Gson().toJson(folderProperties));
        FileWatcherConfiguration.start();
    }
}
