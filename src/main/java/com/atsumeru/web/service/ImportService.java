package com.atsumeru.web.service;

import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.util.StreamUtils;
import com.atsumeru.web.configuration.FileWatcherConfig;
import com.atsumeru.web.exception.NoReadableFoundException;
import com.atsumeru.web.helper.JavaHelper;
import com.atsumeru.web.importer.Importer;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.book.BaseBook;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.importer.FolderProperty;
import com.atsumeru.web.properties.FoldersProperties;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.MetacategoryRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImportService {
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class.getSimpleName());

    public static final String FOLDER_FIELD_NAME = "FOLDER";
    public static final String ARCHIVE_ID_FIELD_NAME = "ARCHIVE_ID";

    private static final AtomicBoolean isImportActive = new AtomicBoolean(false);
    private static final AtomicLong importStartTime = new AtomicLong(0);

    private static final BooksDaoManager daoManager;

    private static ThreadPoolExecutor executorService;

    private static ProgressBar cliProgressBar;

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    public void onStart() {
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Settings.isDisableChapters() ? Runtime.getRuntime().availableProcessors() : 1);
    }

    @Scheduled(fixedDelay = 2500, initialDelay = 3000)
    public static void checkImportState() {
        if (isImportActive() && (executorService.getActiveCount() == 0 && executorService.getQueue().size() == 0 && executorService.getCompletedTaskCount() > 0)) {
            List<String> changedSeriesPaths = FoldersProperties.getFolderProperties()
                    .stream()
                    .filter(property -> GUArray.isNotNull(property.getInLibrarySeries()) && GUArray.isNotNull(property.getInLibraryArchives()))
                    .map(FolderProperty::getAddedSerieFolders)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            if (GUArray.isNotEmpty(changedSeriesPaths)) {
                Importer.calculateVolumesAndChaptersCount(changedSeriesPaths);
            }

            CategoryRepository.reLoadCategories();
            MetacategoryRepository.reIndex();

            daoManager.commit();
            daoManager.setAutoCommit(true);

            cliProgressBar.close();
            System.out.println();
            Importer.resetProgress();
            setImportActive(false);

            AtsumeruCacheManager.evictAll();
            CoversSaverService.saveNonExistentCoversIntoCache();
            FileWatcherConfig.start();
        }
    }

    private static void addIntoQueue(FolderProperty property) {
        executorService.submit(() -> {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
            
            FileWatcherConfig.destroy();

            property.loadInLibraryBooks();
            property.clearAddedFolders();

            daoManager.setAutoCommit(false);
            setImportActive(true);

            startImport(property);
        });
    }

    public static void rescan(boolean reImportIfExist, boolean forceUpdateCovers) {
        new Thread(() -> {
            logger.info(String.format("Requested rescan: re-import: %s, re-cache covers: %s", reImportIfExist, forceUpdateCovers));
            rescanInternal(reImportIfExist, forceUpdateCovers);
        }).start();
    }

    public static boolean rescanSerie(String serieHash) {
        try {
            IBaseBookItem bookItem = BooksRepository.getBookDetails(serieHash);
            FolderProperty tempProperty = new FolderProperty(bookItem.getFolder());
            if (bookItem.isSingle()) {
                tempProperty.setSingles(bookItem.isSingle());
                if (bookItem instanceof BookSerie) {
                    List<BookArchive> archives = daoManager.queryArchivesForSerie((BookSerie) bookItem);
                    if (GUArray.isNotEmpty(archives)) {
                        String archivePath = archives.get(0).getFolder();
                        tempProperty.setSingleArchivePath(archivePath);
                    }
                }
            }

            new Thread(() -> {
                tempProperty.setRecursiveImport(false);
                Importer.resetProgress();
                tempProperty.setReImportIfExist(true);
                tempProperty.setForceUpdateCovers(true);
                Importer.calculateTotal(Collections.singletonList(tempProperty));
                createCliProgressBar(Importer.getTotal());
                addIntoQueue(tempProperty);
            }).start();
            return true;
        } catch (NoReadableFoundException ex) {
            return false;
        }
    }

    public static boolean rescan(String folderHash, boolean rescanFully, boolean forceUpdateCovers) {
        for (FolderProperty property : FoldersProperties.getFolderProperties()) {
            if (property.getHash().equalsIgnoreCase(folderHash)) {
                new Thread(() -> {
                    Importer.resetProgress();
                    property.setReImportIfExist(rescanFully);
                    property.setForceUpdateCovers(forceUpdateCovers);
                    Importer.calculateTotal(Collections.singletonList(property));
                    createCliProgressBar(Importer.getTotal());
                    addIntoQueue(property);
                }).start();
                return true;
            }
        }
        return false;
    }

    private static void rescanInternal(boolean reImportIfExist, boolean forceUpdateCovers) {
        Importer.resetProgress();
        importStartTime.set(System.currentTimeMillis());

        Importer.calculateTotal(FoldersProperties.getFolderProperties());
        createCliProgressBar(Importer.getTotal());

        for (FolderProperty property : FoldersProperties.getFolderProperties()) {
            property.setReImportIfExist(reImportIfExist);
            property.setForceUpdateCovers(forceUpdateCovers);
            addIntoQueue(property);
        }
    }

    private static void startImport(FolderProperty property) {
        Map<String, BookSerie> seriesInDbMap = Importer.getSeriesInFolderFromDb(property);
        Map<String, BookArchive> archivesInDbMap = Importer.getArchivesInFolderFromDb(property);
        Map<String, BookArchive> archivesInFolder = archivesInDbMap.values()
                .stream()
                .filter(StreamUtils.distinctByKey(BaseBook::getFolder))
                .collect(Collectors.toMap(BaseBook::getFolder, Function.identity()));

        Map<String, File> actualArchivesInFS = property.getArchivesInFolder()
                .stream()
                .collect(Collectors.toMap(File::getAbsolutePath, Function.identity()));

        // Поиск Архивов, которых больше нет в файловой системе
        List<IBaseBookItem> archivesNotInFS = archivesInFolder.values()
                .stream()
                .filter(archive -> !actualArchivesInFS.containsKey(archive.getFolder()))
                .collect(Collectors.toList());

        archivesNotInFS.forEach(archive -> {
            archivesInDbMap.remove(archive.getContentId());
            archivesInFolder.remove(archive.getFolder());
        });

        // Поиск Серий, которых больше нет в файловой системе
        List<IBaseBookItem> seriesNotInFS = seriesInDbMap.values()
                .stream()
                .filter(serie -> {
                    File serieFolder = new File(serie.getFolder());
                    return !GUFile.isDirectory(serieFolder) || GUFile.isDirectoryEmpty(serieFolder);
                })
                .collect(Collectors.toList());

        seriesNotInFS.forEach(serie -> seriesInDbMap.remove(serie.getContentId()));

        // Удаление Архивов и Серий (с главами), которых больше нет в файловой системе
        Importer.deleteNotInFileSystemBooks(archivesNotInFS, BookArchive.class);
        Importer.deleteNotInFileSystemBooks(seriesNotInFS, BookSerie.class);

        List<File> files = actualArchivesInFS.values()
                .stream()
                .filter(file -> property.isReImportIfExist()
                        || !archivesInFolder.containsKey(file.getAbsolutePath())
                        || archivesInFolder.get(file.getAbsolutePath()).fileSizeChanged(file))
                .filter(file -> !property.isSingles()
                        || GUString.isEmpty(property.getSingleArchivePath())
                        || GUString.equals(file.getAbsolutePath(), property.getSingleArchivePath()))
                .sorted((file1, file2) -> AlphanumComparator.compareStrings(file1.toString(), file2.toString()))
                .collect(Collectors.toList());

        int skipImport = actualArchivesInFS.size() - files.size();
        cliProgressBar.stepBy(skipImport);

        for (File file : files) {
            try {
                Importer.importFile(property, file, seriesInDbMap, archivesInDbMap, (count, total) -> cliProgressBar.step());
            } catch (Exception ex) {
                ex.printStackTrace();
                cliProgressBar.step();
            }
        }
    }

    public static boolean isImportActive() {
        return isImportActive.get();
    }

    private static void setImportActive(boolean isActive) {
        isImportActive.set(isActive);
    }

    public static long getLastStartTime() {
        return importStartTime.get();
    }

    public static long getRunningMs() {
        return isImportActive()
                ? System.currentTimeMillis() - importStartTime.get()
                : 0;
    }

    public static void add(FolderProperty property) {
        importStartTime.set(System.currentTimeMillis());
        Importer.calculateTotal(Collections.singletonList(property));
        createCliProgressBar(Importer.getTotal());
        addIntoQueue(property);
    }

    public static boolean remove(String folderHash) {
        return FoldersProperties.getFolderProperties()
                .stream()
                .filter(property -> GUString.equalsIgnoreCase(property.getHash(), folderHash))
                .findAny()
                .map(property -> {
                    remove(property);
                    return true;
                })
                .orElse(false);
    }

    public static void remove(FolderProperty property) {
        FoldersProperties.removeFolder(property);

        BooksDaoManager daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
        String deletionColumnValue = property.getPath();

        List<BookArchive> archives = daoManager.queryLike(FOLDER_FIELD_NAME, deletionColumnValue, BookArchive.class);
        int removedArchives = daoManager.removeByColumnLike(FOLDER_FIELD_NAME, deletionColumnValue, BookArchive.class);
        int removedSeries = daoManager.removeByColumnLike(FOLDER_FIELD_NAME, deletionColumnValue, BookSerie.class);
        int removedChapter = daoManager.removeByColumnIn(ARCHIVE_ID_FIELD_NAME, archives.stream().map(BookArchive::getContentId).collect(Collectors.toList()), BookChapter.class);

        logger.info(String.format("Removed %d archives, %d series and %s chapters from DB for path: %s", removedArchives, removedSeries, removedChapter, property.getPath()));
    }

    private static void createCliProgressBar(int max) {
        cliProgressBar = new ProgressBarBuilder()
                .setTaskName("Importing:")
                .setStyle(JavaHelper.isWindows() ? ProgressBarStyle.ASCII : ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .setInitialMax(max)
                .build();
    }
}
