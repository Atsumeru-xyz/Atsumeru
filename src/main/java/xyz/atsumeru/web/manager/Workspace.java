package xyz.atsumeru.web.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;
import xyz.atsumeru.web.AtsumeruApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

@Component
public class Workspace {
    private static final Logger logger = LoggerFactory.getLogger(Workspace.class.getSimpleName());

    private static final String WORKING_DIR = getWorkingDir();

    public static final File DATABASES_DIR = new File(WORKING_DIR, "database");
    public static final File CONFIG_DIR = new File(WORKING_DIR, "config");
    public static final File LOGS_DIR = new File(WORKING_DIR, "logs");
    public static final File CACHE_DIR = new File(WORKING_DIR, "cache");
    public static final File BIN_DIR = new File(WORKING_DIR, "bin");
    public static final File TEMP_DIR = new File(WORKING_DIR, "temp");

    final private static File[] FOLDERS = new File[]{
            DATABASES_DIR,
            CONFIG_DIR,
            LOGS_DIR,
            CACHE_DIR,
            BIN_DIR,
            TEMP_DIR
    };

    public Workspace() {
        checkWorkspace();
    }

    public static void checkWorkspace() {
        logger.info("Checking Workspace...");
        Arrays.stream(FOLDERS)
                .filter(folder -> !Files.isDirectory(folder.toPath()))
                .peek(file -> logger.info("Creating folder: {}", file))
                .forEach(File::mkdirs);
        logger.info("All set!");
    }

    private static String getWorkingDir() {
        return AtsumeruApplication.isInDevMode() ? getInDevModeWorkingDir() : new ApplicationHome(AtsumeruApplication.class).getDir().getAbsolutePath();
    }

    private static String getInDevModeWorkingDir() {
        File workDir = new File(System.getProperty("user.dir"));
        String workDirString = workDir.toString();
        if (workDir.isFile()) {
            workDirString = workDirString.substring(0, workDirString.lastIndexOf(File.separator));
        }
        return workDirString + File.separator;
    }
}