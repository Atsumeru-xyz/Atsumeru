package xyz.atsumeru.web.service;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.configuration.FileWatcherConfiguration;
import xyz.atsumeru.web.exception.NoReadableFoundException;
import xyz.atsumeru.web.helper.JavaHelper;
import xyz.atsumeru.web.importer.Importer;
import xyz.atsumeru.web.manager.Settings;
import xyz.atsumeru.web.manager.cache.AtsumeruCache;
import xyz.atsumeru.web.model.book.BaseBook;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.importer.ImportFolder;
import xyz.atsumeru.web.properties.ImportFolders;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.repository.MetacategoryRepository;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.StreamUtils;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.comparator.AlphanumComparator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@DependsOn("booksDaoManager")
public class ImportService {
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class.getSimpleName());

    public static final String FOLDER_FIELD_NAME = "FOLDER";
    public static final String ARCHIVE_ID_FIELD_NAME = "ARCHIVE_ID";

    private static final AtomicBoolean isImportActive = new AtomicBoolean(false);
    private static final AtomicLong importStartTime = new AtomicLong(0);

    private static ThreadPoolExecutor executorService;
    private static ProgressBar cliProgressBar;

    public ImportService() {
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Settings.isDisableChapters() ? Runtime.getRuntime().availableProcessors() : 1);
    }

    @Scheduled(fixedDelay = 2500, initialDelay = 3000)
    public static void checkImportState() {
        if (isImportActive() && (executorService.getActiveCount() == 0 && executorService.getQueue().size() == 0 && executorService.getCompletedTaskCount() > 0)) {
            List<String> changedSeriesPaths = ImportFolders.getFolderProperties()
                    .stream()
                    .filter(property -> ArrayUtils.isNotNull(property.getInLibrarySeries()) && ArrayUtils.isNotNull(property.getInLibraryArchives()))
                    .map(ImportFolder::getAddedSerieFolders)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            if (ArrayUtils.isNotEmpty(changedSeriesPaths)) {
                Importer.calculateVolumesAndChaptersCount(changedSeriesPaths);
            }

            AtsumeruApplication.getContext().getBean(CategoryRepository.class).reLoadCategories();
            AtsumeruApplication.getContext().getBean(MetacategoryRepository.class).reIndex();

            Beans.getBooksDaoManager().commit();
            Beans.getBooksDaoManager().setAutoCommit(true);

            cliProgressBar.close();
            System.out.println();
            Importer.resetProgress();
            setImportActive(false);

            AtsumeruCache.evictAll();
            CoversSaverService.saveNonExistentCoversIntoCache();
            FileWatcherConfiguration.start();
        }
    }

    private static void addIntoQueue(ImportFolder property) {
        executorService.submit(() -> {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
            
            FileWatcherConfiguration.destroy();

            property.loadInLibraryBooks();
            property.clearAddedFolders();

            Beans.getBooksDaoManager().setAutoCommit(false);
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
            ImportFolder tempProperty = new ImportFolder(bookItem.getFolder());
            if (bookItem.isSingle()) {
                tempProperty.setSingles(bookItem.isSingle());
                if (bookItem instanceof BookSerie bookSerie) {
                    List<BookArchive> archives = Beans.getBooksDaoManager().queryArchivesForSerie(bookSerie);
                    if (ArrayUtils.isNotEmpty(archives)) {
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
        for (ImportFolder property : ImportFolders.getFolderProperties()) {
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

        Importer.calculateTotal(ImportFolders.getFolderProperties());
        createCliProgressBar(Importer.getTotal());

        for (ImportFolder property : ImportFolders.getFolderProperties()) {
            property.setReImportIfExist(reImportIfExist);
            property.setForceUpdateCovers(forceUpdateCovers);
            addIntoQueue(property);
        }
    }

    private static void startImport(ImportFolder property) {
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

        // todo: удаление архивов, которые есть в фс, но их путь не привязан ни к одному пути импорта
        archivesNotInFS.forEach(archive -> {
            archivesInDbMap.remove(archive.getContentId());
            archivesInFolder.remove(archive.getFolder());
        });

        // Поиск Серий, которых больше нет в файловой системе
        List<IBaseBookItem> seriesNotInFS = seriesInDbMap.values()
                .stream()
                .filter(serie -> {
                    File serieFolder = new File(serie.getFolder());
                    return !FileUtils.isDirectory(serieFolder) || FileUtils.isDirectoryEmpty(serieFolder);
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
                        || StringUtils.isEmpty(property.getSingleArchivePath())
                        || StringUtils.equals(file.getAbsolutePath(), property.getSingleArchivePath()))
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

    public static void add(ImportFolder property) {
        importStartTime.set(System.currentTimeMillis());
        Importer.calculateTotal(Collections.singletonList(property));
        createCliProgressBar(Importer.getTotal());
        addIntoQueue(property);
    }

    public static boolean remove(String folderHash) {
        return ImportFolders.getFolderProperties()
                .stream()
                .filter(property -> StringUtils.equalsIgnoreCase(property.getHash(), folderHash))
                .findAny()
                .map(property -> {
                    remove(property);
                    return true;
                })
                .orElse(false);
    }

    public static void remove(ImportFolder property) {
        ImportFolders.removeFolder(property);

        String deletionColumnValue = property.getPath();

        List<BookArchive> archives = Beans.getBooksDaoManager().queryLike(FOLDER_FIELD_NAME, deletionColumnValue, BookArchive.class);
        int removedArchives = Beans.getBooksDaoManager().removeByColumnLike(FOLDER_FIELD_NAME, deletionColumnValue, BookArchive.class);
        int removedSeries = Beans.getBooksDaoManager().removeByColumnLike(FOLDER_FIELD_NAME, deletionColumnValue, BookSerie.class);
        int removedChapter = Beans.getBooksDaoManager().removeByColumnIn(ARCHIVE_ID_FIELD_NAME, archives.stream().map(BookArchive::getContentId).collect(Collectors.toList()), BookChapter.class);

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
