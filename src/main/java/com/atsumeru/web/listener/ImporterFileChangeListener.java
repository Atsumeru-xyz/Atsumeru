package com.atsumeru.web.listener;

import com.atsumeru.web.filewatch.ChangedFile;
import com.atsumeru.web.filewatch.ChangedFiles;
import com.atsumeru.web.filewatch.FileChangeListener;
import com.atsumeru.web.service.CoversSaverService;
import com.atsumeru.web.service.ImportService;
import com.atsumeru.web.service.MetadataUpdateService;
import com.atsumeru.web.manager.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Component
public class ImporterFileChangeListener implements FileChangeListener {
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
                .ifPresent(file -> ImportService.rescan( false, false));
    }

    private boolean isLocked(Path path) {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.WRITE); FileLock lock = ch.tryLock()) {
            return lock == null;
        } catch (IOException e) {
            return true;
        }
    }
}