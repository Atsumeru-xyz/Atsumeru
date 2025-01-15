package xyz.atsumeru.web.configuration;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import xyz.atsumeru.web.manager.Settings;
import xyz.atsumeru.web.manager.fswatcher.ChangedFile;
import xyz.atsumeru.web.manager.fswatcher.ChangedFiles;
import xyz.atsumeru.web.manager.fswatcher.FileChangeListener;
import xyz.atsumeru.web.manager.fswatcher.FileSystemWatcher;
import xyz.atsumeru.web.properties.ImportFolders;
import xyz.atsumeru.web.service.CoversSaverService;
import xyz.atsumeru.web.service.ImportService;
import xyz.atsumeru.web.service.MetadataUpdateService;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Configuration
@DependsOn({"importFolders", "settings"})
public class FileWatcherConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(FileWatcherConfiguration.class.getSimpleName());
    private static FileSystemWatcher fsWatcher;

    public FileWatcherConfiguration() {
        create();
    }

    public FileSystemWatcher create() {
        if (!Settings.isDisableFileWatcher()) {
            fsWatcher = new FileSystemWatcher(true, Duration.ofSeconds(20), Duration.ofSeconds(3));
            fsWatcher.addListener(new ImporterFileChangeListener());
            startWatcherThread();
        } else {
            logger.warn("Import Filesystem Watcher disabled");
        }
        return fsWatcher;
    }

    private static void startWatcherThread() {
        new Thread(() -> {
            logger.info("Starting Import Filesystem Watcher...");
            if (fsWatcher != null) {
                fsWatcher.clearSourceFolders();
                ImportFolders.getFolderProperties().forEach(property -> fsWatcher.addSourceFolder(new File(property.getPath())));
                fsWatcher.start();
                logger.info("Import Filesystem Watcher started!");
            } else {
                logger.warn("Unable to star Filesystem Watcher!");
            }
        }).start();
    }

    public static void start() {
        destroy();
        startWatcherThread();
    }

    @PreDestroy
    public static void destroy() {
        Optional.ofNullable(fsWatcher).ifPresent(FileSystemWatcher::stop);
    }

    private static class ImporterFileChangeListener implements FileChangeListener {
        private static final Logger logger = LoggerFactory.getLogger(ImporterFileChangeListener.class.getSimpleName());
        private static final Predicate<ChangedFiles> serverNotLocked = notLocked ->
                !MetadataUpdateService.isUpdateActive() && !ImportService.isImportActive() && !CoversSaverService.isCachingActive();

        @Override
        public void onChange(Set<ChangedFiles> changeSet) {
            AtomicBoolean isLogged = new AtomicBoolean();
            changeSet.stream()
                    .filter(serverNotLocked)
                    .flatMap(changedFiles -> changedFiles.getFiles().stream())
                    .filter(changedFile -> !isLocked(changedFile.getFile().toPath()))
                    .filter(changedFile -> !Settings.isDisableWatchForModifiedFiles() || changedFile.getType() != ChangedFile.Type.MODIFY)
                    .peek(changedFile -> {
                        if (!isLogged.get()) {
                            logger.info("Requested Importer rescan because of filesystem changes with type [" + changedFile.getType() + "]");
                            isLogged.set(true);
                        }
                    })
                    .limit(1)
                    .findFirst()
                    .ifPresent(file -> ImportService.rescan(false, false));
        }

        private boolean isLocked(Path path) {
            try (FileChannel ch = FileChannel.open(path, StandardOpenOption.WRITE); FileLock lock = ch.tryLock()) {
                return lock == null;
            } catch (IOException e) {
                return true;
            }
        }
    }
}