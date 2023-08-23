package com.atsumeru.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@Service
public class Workspace {
    private static final Logger logger = LoggerFactory.getLogger(Workspace.class.getSimpleName());

    public static final String WORKING_DIR = getWorkingDir();
    public static final String DATABASES_DIR = WORKING_DIR + "database" + File.separator;
    public static final String CONFIG_DIR = WORKING_DIR + "config" + File.separator;
    public static final String LOGS_DIR = WORKING_DIR + "logs" + File.separator;
    public static final String CACHE_DIR = WORKING_DIR + "cache" + File.separator;
    public static final String BIN_DIR = WORKING_DIR + "bin" + File.separator;
    public static final String TEMP_DIR = WORKING_DIR + "temp" + File.separator;

    final private static String[] FOLDERS_TO_CREATE = new String[]{"database", "config", "cache", "logs", "bin", "temp"};

    public static void configureWorkspace() {
        Arrays.stream(FOLDERS_TO_CREATE)
                .filter(folder -> !Files.isDirectory(Paths.get(WORKING_DIR + folder)))
                .forEach(Workspace::createFolder);
    }

    private static void createFolder(String folder) {
        new File(WORKING_DIR + folder).mkdirs();
    }

    private static String getWorkingDir() {
        File workDir = new File(System.getProperty("user.dir"));
        String workDirString = workDir.toString();
        if (workDir.isFile()) {
            workDirString = workDirString.substring(0, workDirString.lastIndexOf(File.separator));
        }
        return workDirString + File.separator;
    }

    public static void moveLegacyConfig(File legacyFile, File newFile) {
        if (FileUtils.isFile(legacyFile)) {
            try {
                Files.move(legacyFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Unable to move legacy config (" + legacyFile + ")");
            }
        }
    }
}