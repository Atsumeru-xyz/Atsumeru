package com.atsumeru.web.configuration;

import com.atsumeru.web.filewatch.FileSystemWatcher;
import com.atsumeru.web.listener.ImporterFileChangeListener;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.properties.FoldersProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import javax.annotation.PreDestroy;
import java.io.File;
import java.time.Duration;
import java.util.Optional;

@Configuration
public class FileWatcherConfig {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcherConfig.class.getSimpleName());
    private static FileSystemWatcher fsWatcher;

    public static void start() {
        destroy();
        new FileWatcherConfig().fileSystemWatcher();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    public FileSystemWatcher fileSystemWatcher() {
        if (!Settings.isDisableFileWatcher()) {
            fsWatcher = new FileSystemWatcher(true, Duration.ofSeconds(20), Duration.ofSeconds(3));
            FoldersProperties.getFolderProperties().forEach(property -> fsWatcher.addSourceFolder(new File(property.getPath())));
            fsWatcher.addListener(new ImporterFileChangeListener());
            logger.info("Starting Import Filesystem Watcher...");
            new Thread(() -> {
                fsWatcher.start();
                logger.info("Import Filesystem Watcher started!");
            }).start();
        } else {
            logger.warn("Import Filesystem Watcher disabled");
        }
        return fsWatcher;
    }

    @PreDestroy
    public static void destroy() {
        Optional.ofNullable(fsWatcher).ifPresent(FileSystemWatcher::stop);
    }
}