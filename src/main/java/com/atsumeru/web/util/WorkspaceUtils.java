package com.atsumeru.web.util;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Service
public class WorkspaceUtils {
    public static final String WORKING_DIR = getWorkingDir();
    public static final String DATABASES_DIR = WORKING_DIR + "database" + File.separator;
    public static final String LOGS_DIR = WORKING_DIR + "logs" + File.separator;
    public static final String CACHE_DIR = WORKING_DIR + "cache" + File.separator;
    public static final String BIN_DIR = WORKING_DIR + "bin" + File.separator;
    public static final String TEMP_DIR = WORKING_DIR + "temp" + File.separator;

    final private static String[] FOLDERS_TO_CREATE =  new String[] {"database", "cache", "logs", "bin", "temp"};

    public static void configureWorkspace() {
        Arrays.stream(FOLDERS_TO_CREATE)
                .filter(folder -> !Files.isDirectory(Paths.get(WORKING_DIR + folder)))
                .forEach(WorkspaceUtils::createFolder);
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
}