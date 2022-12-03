package com.atsumeru.web.service;

import com.atsumeru.web.configuration.FileWatcherConfig;
import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.helper.FilesHelper;
import com.atsumeru.web.metadata.BookInfo;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.importer.ReadableContent;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.util.ContentDetector;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.archive.ArchiveReader;
import com.atsumeru.web.archive.iterator.IArchiveIterator;
import com.atsumeru.web.enums.BookType;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.helper.HashHelper;
import com.atsumeru.web.helper.JavaHelper;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.util.GUFile;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Component
public class MetadataUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(MetadataUpdateService.class.getSimpleName());
    private static final Gson gson = new Gson()
            .newBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    String fieldName = fieldAttributes.getName();
                    return fieldName.equals("folder")
                            || fieldName.equals("pagesCount")
                            || fieldName.equals("createdAt")
                            || fieldName.equals("updatedAt")
                            || fieldName.equals("history");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private static MetadataUpdateService INSTANCE;
    private static final AtomicBoolean isUpdateActive = new AtomicBoolean(false);
    private static final AtomicLong updateStartTime = new AtomicLong();

    private static final AtomicInteger threadId = new AtomicInteger();
    private static final AtomicInteger progress = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();

    private final BooksDaoManager daoManager = BooksDatabaseRepository.getInstance().getDaoManager();

    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    public void startService() {
        INSTANCE = new MetadataUpdateService();
        logger.info("Metadata service started");
    }

    public static MetadataUpdateService getInstance() {
        return INSTANCE;
    }

    public static boolean isUpdateActive() {
        return isUpdateActive.get();
    }

    public static long getRunningMs() {
        return isUpdateActive()
                ? System.currentTimeMillis() - updateStartTime.get()
                : 0;
    }

    public static int getProgress() {
        return progress.get();
    }

    public static int getTotal() {
        return total.get();
    }

    public static void incrementProgress() {
        progress.set(progress.get() + 1);
    }

    private static void resetCounters(int total) {
        progress.set(0);
        MetadataUpdateService.total.set(total);
    }

    private static boolean startInThread(Runnable runnable, String taskName) {
        if (isUpdateActive() || ImportService.isImportActive() || CoversSaverService.isCachingActive()) {
            return false;
        }

        new Thread(() -> {
            onThreadStart(taskName);
            runnable.run();
            onThreadFinish();
        }, MetadataUpdateService.class.getSimpleName() + threadId.incrementAndGet()).start();
        return true;
    }

    private static void onThreadStart(String taskName) {
        FileWatcherConfig.destroy();

        logger.info("Started " + taskName + " in thread: " + Thread.currentThread().getName());
        isUpdateActive.set(true);
        updateStartTime.set(System.currentTimeMillis());
    }

    private static void onThreadFinish() {
        isUpdateActive.set(false);

        AtsumeruCacheManager.evictAll();
        FileWatcherConfig.start();
    }

    public boolean startUpdateForSerie(BookSerie bookSeries, boolean insertIntoArchive, boolean insertIntoDBOnly) {
        return startInThread(() -> saveIntoArchives(bookSeries, insertIntoArchive, insertIntoDBOnly), "metadata update");
    }

    public boolean startUpdateForArchive(BookArchive bookArchive, boolean insertIntoArchive, boolean insertIntoDBOnly) {
        return startInThread(() -> saveIntoArchive(bookArchive, insertIntoArchive, insertIntoDBOnly, true), "metadata update");
    }

    public boolean startCreatingUniqueIds(boolean insertIntoArchives, boolean insertIntoDBOnly, boolean forceCreate) {
        return startInThread(() -> createUniqueIds(insertIntoArchives, insertIntoDBOnly, forceCreate), "creating unique ids");
    }

    public boolean startInjectAllFromDatabase() {
        return startInThread(this::injectAllFromDatabase, "injecting metadata from database into archives");
    }

    private void injectAllFromDatabase() {
        List<BookArchive> archives = daoManager.queryAll(BookArchive.class, LibraryPresentation.ARCHIVES);
        resetCounters(archives.size());
        archives.forEach(archive -> saveIntoArchive(archive, true, false, false));
    }

    private void createUniqueIds(boolean insertIntoArchive, boolean insertIntoDBOnly, boolean forceCreate) {
        List<BookSerie> series = daoManager.queryAll(BookSerie.class, LibraryPresentation.ARCHIVES);

        ProgressBar cliProgressBar = new ProgressBarBuilder()
                .setTaskName("Generating unique hashes:")
                .setInitialMax(series.size())
                .setStyle(JavaHelper.isWindows() ? ProgressBarStyle.ASCII : ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build();

        resetCounters(series.size());
        for (BookSerie serie : series) {
            logger.info("Creating unique ids for serie: " + serie.getTitle() + " (" + serie.getFolder() + ")");
            List<BookArchive> archives = daoManager.query("SERIE", String.valueOf(serie.getDbId()), BookArchive.class);
            // Create new Serie Hash
            serie.setSerieId(HashHelper.getMHash2(Constants.SERIE_HASH_TAG, UUID.randomUUID().toString()));
            serie.setCover(serie.getSerieId());
            for (BookArchive archive : archives) {
                if (!archive.getIsUniqueContentID() || forceCreate) {
                    logger.info("Creating unique ids for archive: " + archive.getTitle() + " (" + archive.getFolder() + ")");
                    // Load Chapters and History for associated Archive
                    List<History> historyList = daoManager.getHistoryDao().queryByHash(archive.getContentId(), BookArchive.class);

                    // Create new Archive Hash
                    archive.setContentId(HashHelper.getMHash2(Constants.ARCHIVE_HASH_TAG, UUID.randomUUID().toString()));
                    archive.setSerieHash(serie.getSerieId());
                    archive.setCover(archive.getContentId());
                    archive.setIsUniqueContentID(true);

                    // Save into archive
                    boolean isSaved = save(serie, archive, insertIntoArchive, insertIntoDBOnly);
                    if (isSaved) {
                        // Save Serie and Archive into database
                        daoManager.save(serie);
                        daoManager.save(archive);

                        // Generate new Chapter Hash
                        if (!Settings.isDisableChapters()) {
                            archive.getChapters().forEach(chapter -> {
                                String oldHash = chapter.getChapterId();
                                chapter.setSerieId(serie.getContentId());
                                chapter.setArchiveId(archive.getContentId());
                                chapter.generateChapterId(archive.getContentId());

                                // Update History assigned Chapter hash
                                for (History history : historyList) {
                                    if (GUString.equalsIgnoreCase(history.getChapterHash(), oldHash)) {
                                        history.setChapterHash(chapter.getChapterId());
                                    }
                                }

                                // Update Chapters assigned hashes and save into Database
                                daoManager.save(chapter);
                            });
                        }

                        // Save new hashes into file attributes
                        Path archivePath = Paths.get(archive.getFolder());
                        FilesHelper.writeHashFileAttribute(archivePath, Constants.ATTRIBUTE_HASH, archive.getContentId());
                        FilesHelper.writeHashFileAttribute(archivePath, Constants.ATTRIBUTE_SERIE_HASH, serie.getContentId());

                        // Update History assigned Serie/Archive hashes and save into Database
                        for (History history : historyList) {
                            history.setSerieHash(serie.getContentId());
                            history.setArchiveHash(archive.getContentId());
                            daoManager.getHistoryDao().save(history);
                        }
                    }
                }
            }

            incrementProgress();
            cliProgressBar.step();
        }
        cliProgressBar.close();

        logger.info(String.format("Ended creating unique hashes in thread: %s. Took: %sms", Thread.currentThread().getName(), getRunningMs()));
        CoversSaverService.saveNonExistentCoversIntoCache();
    }

    private void saveIntoArchives(BookSerie bookSerie, boolean insertIntoArchive, boolean insertIntoDBOnly) {
        List<BookArchive> archives = daoManager.query("SERIE", String.valueOf(bookSerie.getDbId()), BookArchive.class);
        resetCounters(archives.size());
        for (BookArchive archive : archives) {
            logger.info(String.format("Updating metadata for archive %s with hash %s", archive.getTitle(), archive.getContentId()));
            archive.copyFromBaseBook(bookSerie);
            daoManager.save(archive);

            boolean isSaved = save(bookSerie, archive, insertIntoArchive, insertIntoDBOnly);
            if (isSaved) {
                logger.info("Metadata was updated");
            } else {
                logger.error("Unable to update metadata");
            }

            incrementProgress();
        }

        logger.info(String.format("Ended metadata update in thread: %s. Took: %sms", Thread.currentThread().getName(), getRunningMs()));
    }

    private void saveIntoArchive(BookArchive bookArchive, boolean insertIntoArchive, boolean insertIntoDBOnly, boolean resetCounters) {
        if (resetCounters) {
            resetCounters(1);
        }

        logger.info(String.format("Updating metadata for archive %s with hash %s", bookArchive.getTitle(), bookArchive.getContentId()));

        boolean isSaved = save(bookArchive.getSerie(), bookArchive, insertIntoArchive, insertIntoDBOnly);
        if (isSaved) {
            logger.info("Metadata was updated");
        } else {
            logger.error("Unable to update metadata");
        }

        incrementProgress();

        logger.info(String.format("Ended metadata update in thread: %s. Took: %sms", Thread.currentThread().getName(), getRunningMs()));
    }

    private boolean save(BookSerie serie, BookArchive archive, boolean insertIntoArchive, boolean insertIntoDBOnly) {
        boolean isSaved = insertIntoArchive(serie, archive, insertIntoArchive);
        if (isSaved) {
            deleteExternalMetadata(archive);
        }
        if (!isSaved && (!insertIntoArchive || archive.isBook()) && !insertIntoDBOnly) {
            isSaved = saveIntoDirectory(serie, archive);
        }
        return isSaved || insertIntoDBOnly;
    }

    private boolean insertIntoArchive(BookSerie serie, BookArchive archive, boolean insertIntoArchive) {
        BookType bookType = ContentDetector.detectBookType(Paths.get(archive.getFolder()));
        boolean isArchiveFile = bookType == BookType.ARCHIVE || bookType == BookType.EPUB;

        if (isArchiveFile && insertIntoArchive) {
            IArchiveIterator archiveIterator = createArchiveIterator(archive);
            if (archiveIterator != null) {
                Map<String, String> contentToSave = new HashMap<>();
                contentToSave.put(ReadableContent.BOOK_JSON_INFO_FILENAME, BookInfo.toJSON(archive, serie.getBoundServices()).toString(4));
                if (!Settings.isDisableChapters()) {
                    archive.getChapters().forEach(chapter -> contentToSave.put(
                            GUFile.addPathSlash(chapter.getFolder()) + ReadableContent.CHAPTER_JSON_INFO_FILENAME,
                            BookInfo.toJson(chapter).toString(4)
                    ));
                }
                return archiveIterator.saveIntoArchive(archive.getFolder(), contentToSave);
            }
        }
        return false;
    }

    private boolean saveIntoDirectory(BookSerie serie, BookArchive archive) {
        File contentFolder = getContentMetadataFolder(archive);
        contentFolder.mkdirs();

        boolean isSaved = GUFile.writeStringToFile(
                new File(contentFolder, ReadableContent.BOOK_JSON_INFO_FILENAME),
                BookInfo.toJSON(archive, serie.getBoundServices()).toString(4)
        );

        if (!Settings.isDisableChapters()) {
            for (BookChapter chapter : archive.getChapters()) {
                File chapterFolder = new File(contentFolder, Optional.ofNullable(chapter.getFolder()).orElse(""));
                chapterFolder.mkdirs();
                isSaved = isSaved && GUFile.writeStringToFile(
                        new File(chapterFolder, ReadableContent.CHAPTER_JSON_INFO_FILENAME),
                        BookInfo.toJson(chapter).toString(4)
                );
            }
        }

        return isSaved;
    }

    private void deleteExternalMetadata(BookArchive archive) {
        try {
            File contentFolder = getContentMetadataFolder(archive);
            new File(contentFolder, ReadableContent.BOOK_JSON_INFO_FILENAME).delete();

            if (!Settings.isDisableChapters()) {
                for (BookChapter chapter : archive.getChapters()) {
                    File chapterFolder = new File(contentFolder, chapter.getFolder());
                    new File(chapterFolder, ReadableContent.CHAPTER_JSON_INFO_FILENAME).delete();
                }
            }
        } catch (Exception ex) {
            logger.warn("Unable to delete external metadata. Reason: " + ex.getMessage());
        }
    }

    private File getContentMetadataFolder(BookArchive archive) {
        File atsumeruFolder = new File(new File(archive.getFolder()).getParent(), ReadableContent.EXTERNAL_INFO_DIRECTORY_NAME);
        return new File(atsumeruFolder, GUFile.getFileName(archive.getFolder()));
    }

    private IArchiveIterator createArchiveIterator(BookArchive archive) {
        try {
            return ArchiveReader.getArchiveIterator(archive.getFolder());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
