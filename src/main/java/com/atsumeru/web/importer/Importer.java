package com.atsumeru.web.importer;

import com.atsumeru.web.enums.Status;
import com.atsumeru.web.helper.ChapterRecognition;
import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.helper.JavaHelper;
import com.atsumeru.web.importer.listener.OnImportCallback;
import com.atsumeru.web.logger.FileLogger;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.model.importer.FolderProperty;
import com.atsumeru.web.model.importer.ReadableContent;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.HistoryRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.service.ImportService;
import com.atsumeru.web.util.*;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import kotlin.Pair;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Importer {
    private static final Logger logger = LoggerFactory.getLogger(Importer.class.getSimpleName());
    public static final java.util.logging.Logger fileLogger;

    private static final BooksDaoManager daoManager;
    private static final AtomicInteger progress = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
        fileLogger = FileLogger.createLogger("ImporterLog", WorkspaceUtils.LOGS_DIR + "import.log");
    }

    public static void resetProgress() {
        progress.set(0);
        total.set(0);
    }

    public static int getProgress() {
        return progress.get();
    }

    public static int getTotal() {
        return total.get();
    }

    public static void incrementAndNotifyProgress(OnImportCallback listener) {
        incrementProgress();
        listener.onProgressChanged(progress.get(), total.get());
    }

    public static void incrementProgress() {
        progress.set(progress.get() + 1);
    }

    public static void incrementTotal(int count) {
        total.set(total.get() + count);
    }

    public static void calculateTotal(List<FolderProperty> properties) {
        incrementTotal(
                properties.stream()
                        .map(FolderProperty::getArchivesInFolder)
                        .mapToInt(List::size)
                        .reduce(0, Integer::sum)
        );
    }

    public static void importFile(FolderProperty property, File file, Map<String, BookSerie> seriesMap, Map<String, BookArchive> archivesMap, OnImportCallback callback) {
        boolean reImportIfExist = property.isReImportIfExist();
        boolean forceUpdateCovers = property.isForceUpdateCovers();

        String importPath = GUFile.removeLastPathSlash(property.getPath());

        String archivePath = file.getPath();
        String parentPath = file.getParent();

        boolean asSingles = property.isAsSingles(file, parentPath.equals(importPath));

        // Чтение архива и заполнение модели ReadableContent информацией из него
        ReadableContent readableContent = Importer.importFile(
                archivesMap,
                parentPath,
                archivePath,
                asSingles,
                reImportIfExist,
                ignoreVolumeNumbersDetection,
                forceUpdateCovers
        );

        if (readableContent != null) {
            String existedSerieHash = seriesMap.entrySet()
                    .stream()
                    .filter(entry -> !(asSingles || readableContent.getBookArchive().isSingle() && readableContent.getSerieArchive() == null)
                            ? GUString.equalsIgnoreCase(GUFile.removeLastPathSlash(entry.getValue().getFolder()), GUFile.removeLastPathSlash(parentPath))
                            : GUString.equalsIgnoreCase(GUFile.removeLastPathSlash(entry.getKey()), readableContent.getSerieHash()))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (GUString.isNotEmpty(existedSerieHash)) {
                readableContent.setSerieHash(existedSerieHash);
            }

            Pair<String, String> serieArchiveFolderPair = saveArchive(readableContent, seriesMap, parentPath, reImportIfExist);

            // Распределяем в Set'ы пути к импортированной Серии и Архиву
            if (serieArchiveFolderPair != null) {
                property.addSerieFolder(serieArchiveFolderPair.getFirst());
                property.addArchiveFolder(serieArchiveFolderPair.getSecond());
            }
        }

        incrementAndNotifyProgress(callback);
    }

    @Nullable
    public static ReadableContent importFile(Map<String, BookArchive> archivesMap, String parentPath, String archivePath, boolean asSingle, boolean reImportIfExist, boolean forceUpdateCovers) {
        try {
            // Создание модели ReadableContent и наполнение ее данными из файла
            return ReadableContent.create(archivesMap, parentPath, archivePath, asSingle, reImportIfExist, forceUpdateCovers);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void deleteNotInFileSystemBooks(List<IBaseBookItem> wasInDb, Set<String> existFolderPaths, Class<? extends IBaseBookItem> clazz) {
        deleteNotInFileSystemBooks(getNotInFileSystemBooks(wasInDb, existFolderPaths), clazz);
    }

    public static void deleteNotInFileSystemBooks(List<IBaseBookItem> notInFileSystemBooks, Class<? extends IBaseBookItem> clazz) {
        String bookType = clazz.isAssignableFrom(BookSerie.class) ? "serie" : "archive";

        List<String> bookPaths = notInFileSystemBooks.stream()
                .peek(bookItem -> logFile(String.format("Removing (%s) %s from DB with path: %s...\n", bookType, bookItem.getTitle(), bookItem.getFolder())))
                .map(IBaseBookItem::getFolder)
                .collect(Collectors.toList());

        if (GUArray.isNotEmpty(bookPaths)) {
            int removed = daoManager.removeByColumnIn("FOLDER", bookPaths, clazz);

            if (clazz.isAssignableFrom(BookArchive.class)) {
                List<String> serieIds = notInFileSystemBooks.stream()
                        .filter(book -> book.getStatus() == Status.SINGLE)
                        .map(IBaseBookItem::getSerieDbId)
                        .map(Object::toString)
                        .collect(Collectors.toList());

                        if (GUArray.isNotEmpty(serieIds)) {
                            daoManager.removeByColumnIn("ID", serieIds, BookSerie.class);
                        }
            }

            List<String> contentIds = notInFileSystemBooks.stream()
                    .map(IBaseBookItem::getContentId)
                    .collect(Collectors.toList());

            int removedChapters = daoManager.removeByColumnIn("SERIE_ID", contentIds, BookChapter.class)
                    + daoManager.removeByColumnIn("ARCHIVE_ID", contentIds, BookChapter.class);

            logWarn("Removed (" + removed + ") " + bookType + "s and (" + removedChapters + ") corresponding chapters that not exist on file system");
        }
    }

    private static List<IBaseBookItem> getNotInFileSystemBooks(List<IBaseBookItem> wasInDb, Set<String> existFolderPaths) {
        return wasInDb.stream()
                .filter(bookItem -> !existFolderPaths.contains(bookItem.getFolder()))
                .collect(Collectors.toList());
    }

    public static List<File> listArchives(String importPath, boolean recursive) {
        return FileUtils.getAllFilesFromDirectory(importPath, Constants.SUPPORTED_SINGLE_FILES, recursive)
                .stream()
                .sorted(AlphanumComparator::compareObjToString)
                .collect(Collectors.toList());
    }

    private static Pair<String, String> saveArchive(ReadableContent readableContent, Map<String, BookSerie> seriesMap, String parentPath, boolean reImportIfExist) {
        Pair<String, String> pair = null;
        if (readableContent != null && (GUArray.isNotEmpty(readableContent.getPageEntryNames()) || readableContent.isBookFile())) {
            BookArchive bookArchive = daoManager.queryItem(readableContent.getBookArchive().getContentId(), BookArchive.class);
            BookSerie bookSerie = saveArchiveAndCreateSerie(readableContent, bookArchive, seriesMap, parentPath, reImportIfExist);
            if (!readableContent.isBookFile()) {
                saveChaptersForArchive(readableContent, bookSerie, bookArchive, reImportIfExist);
                fixHistory(bookArchive);
            }
            pair = new Pair<>(bookSerie.getFolder(), bookArchive.getFolder());
        } else {
            logError("Unable to read archive: " + (readableContent != null ? readableContent.getArchivePath() : parentPath));
        }
        logFile("---------------------------------------------");
        return pair;
    }

    private static BookSerie saveArchiveAndCreateSerie(ReadableContent readableContent, BookArchive bookArchiveInDb,
                                                       Map<String, BookSerie> seriesMap, String parentPath, boolean reImportIfExist) {
        BookSerie bookSerie;

        String serieHash = readableContent.getSerieHash();
        boolean asSingle = readableContent.isAsSingle() || readableContent.getBookArchive().isSingle() && readableContent.getSerieArchive() == null;
        boolean isBook = readableContent.isBookFile();

        bookArchiveInDb.fromBaseBook(readableContent.getBookArchive());
        bookArchiveInDb.setCover(readableContent.getBookArchive().getCover());
        bookArchiveInDb.setIsBook(isBook);
        bookArchiveInDb.updateTimestamps();

        if (!isBook) {
            bookArchiveInDb.setPageEntryNames(readableContent.getPageEntryNames());
        }

        if (seriesMap.containsKey(serieHash)) {
            bookSerie = seriesMap.get(serieHash);
            bookSerie.updateTimestamps();

            if (!bookSerie.isSerieUpdatedInImport() && reImportIfExist) { // && readableContent.isHasMetadata()
                updateSerieFromBookArchive(
                        bookSerie,
                        bookArchiveInDb,
                        serieHash,
                        parentPath,
                        readableContent.getSerieCoverStream() != null,
                        asSingle,
                        readableContent.isHasMetadata()
                );
            } else {
                bookSerie.setPagesCount(!isBook
                        ? bookSerie.getPagesCount() + bookArchiveInDb.getPagesCount()
                        : readableContent.getBookArchive().getPagesCount());
            }
        } else {
            seriesMap.put(
                    serieHash,
                    bookSerie = createSerieFromBookArchiveOrExternalInfo(
                            readableContent,
                            bookArchiveInDb,
                            serieHash,
                            parentPath,
                            readableContent.getSerieCoverStream() != null,
                            asSingle,
                            readableContent.isHasMetadata()
                    )
            );
        }

        if (bookSerie.getDbId() != null) {
            bookArchiveInDb.setSerie(bookSerie);
            daoManager.save(bookArchiveInDb);

            // Считаем новое количество томов в Серии
            bookSerie.setVolumesCount(daoManager.countArchivesForSerie(bookSerie));
            daoManager.save(bookSerie);
        } else {
            daoManager.save(bookSerie);
            bookArchiveInDb.setSerie(bookSerie);
            daoManager.save(bookArchiveInDb);
        }

        return bookSerie;
    }

    private static void fixHistory(BookArchive bookArchive) {
        long time = System.currentTimeMillis();

        List<History> historyList = HistoryRepository.getBookHistory(bookArchive.getContentId(), BookArchive.class)
                .stream()
                .filter(history -> GUString.isNotEmpty(history.getChapterHash()))
                .collect(Collectors.toList());

        // Исправление записей Серий/Архивов
        long fixed = fixHistoryEntries(historyList, bookArchive.getPagesCount());

        // Исправление записей Глав
        fixed += historyList.stream()
                .mapToLong(history -> {
                    int chapterPagesCount = bookArchive.getChapters()
                            .stream()
                            .filter(chapter -> GUString.equalsIgnoreCase(chapter.getChapterId(), history.getChapterHash()))
                            .mapToInt(BookChapter::getPagesCount)
                            .findFirst()
                            .orElse(0);

                    return chapterPagesCount > 0
                            ? fixHistoryEntries(Collections.singletonList(history), chapterPagesCount)
                            : 0;
                })
                .reduce(0, Long::sum);

        if (fixed > 0) {
            log("Fixed (" + fixed + ") pages history entries. Fixing time: " + (System.currentTimeMillis() - time) + "ms");
        }
    }

    private static long fixHistoryEntries(List<History> historyList, int pagesCount) {
        return historyList.stream()
                .filter(history -> history.getPagesCount() == null || history.getPagesCount() != pagesCount && pagesCount > 0)
                .peek(history -> {
                    history.setPagesCount(pagesCount);
                    if (history.getCurrentPage() == null) {
                        history.setCurrentPage(0);
                    } else if (history.getCurrentPage() > pagesCount) {
                        history.setCurrentPage(pagesCount);
                    }

                    daoManager.save(history);
                })
                .count();
    }

    private static BookSerie createSerieFromBookArchiveOrExternalInfo(ReadableContent readableContent, BookArchive bookArchive, String serieHash, String parentPath, boolean hasSerieCover, boolean asSingles, boolean isHasMetadata) {
        return updateSerieFromBookArchive(new BookSerie(), Optional.ofNullable(readableContent.getSerieArchive()).orElse(bookArchive), serieHash, parentPath, hasSerieCover, asSingles, isHasMetadata);
    }

    private static BookSerie updateSerieFromBookArchive(BookSerie bookSerie, BookArchive bookArchive, String serieHash, String parentPath, boolean hasSerieCover, boolean asSingles, boolean isHasMetadata) {
        bookSerie.fromBaseBook(bookArchive);
        if (GUString.isEmpty(bookSerie.getTitle()) || !isHasMetadata && !asSingles) {
            bookSerie.setTitle(GUFile.getCurrentDirName(parentPath));
        }
        bookSerie.setSerieId(serieHash);
        bookSerie.setFolder(parentPath);
        bookSerie.setCover(hasSerieCover ? serieHash : bookArchive.getContentId());

        Status status = bookArchive.getStatus();
        if (asSingles) {
            status = status == Status.ANTHOLOGY ? status : Status.SINGLE;
        } else if (status == Status.UNKNOWN) {
            status = Status.ONGOING;
        }

        bookSerie.setPagesCount(bookArchive.getPagesCount());
        bookSerie.setIsSingle(asSingles);
        bookSerie.setStatus(!asSingles && status == Status.SINGLE ? Status.UNKNOWN.name() : status.name());
        bookSerie.setSerieUpdatedInImport(true);
        return bookSerie;
    }

    private static void saveChaptersForArchive(ReadableContent archive, BookSerie bookSerie, BookArchive bookArchive, boolean reImportIfExist) {
        if (!Settings.isDisableChapters()) {
            Set<String> chapterIds = bookArchive.getChapterIds();
            List<BookChapter> chapters = archive.getChapters()
                    .stream()
                    .filter(chapter -> isSkipChapter(chapterIds, chapter, reImportIfExist))
                    .peek(chapter -> {
                        chapter.setSerie(bookSerie);
                        chapter.setArchive(bookArchive);
                        if (chapter.getUpdatedAt() == null || chapter.getUpdatedAt() <= 0) {
                            chapter.setUpdatedAt(System.currentTimeMillis());
                        }

                        // Парсинг номеров глав
                        ChapterRecognition.parseNumbers(bookArchive, chapter);
                    })
                    .peek(chapter -> logFile("Chapter with title = '" + chapter.getTitle() + "' added into db"))
                    .collect(Collectors.toList());

            if (GUArray.isNotEmpty(chapters)) {
                bookArchive.setChapters(chapters);
                daoManager.save(bookArchive);
            }
        }
    }

    private static boolean isSkipChapter(Set<String> chapterIds, BookChapter chapter, boolean reImportIfExist) {
        if (!reImportIfExist && chapterIds.contains(chapter.getChapterId())) {
            logFile("Chapter with title = '" + chapter.getTitle() + "' and id = '" + chapter.getChapterId() + "' from archive already in db. Skipping...");
            return false;
        }
        return true;
    }

    public static Map<String, BookSerie> getSeriesInFolderFromDb(FolderProperty property) {
        List<BookSerie> seriesList = daoManager.queryLike(ImportService.FOLDER_FIELD_NAME, property.getPath(), BookSerie.class);
        return seriesList.stream()
                .peek(serie -> serie.setSerieUpdatedInImport(false))
                .collect(Collectors.toMap(BookSerie::getContentId, Function.identity()));
    }

    public static Map<String, BookArchive> getArchivesInFolderFromDb(FolderProperty property) {
        List<BookArchive> archivesList = daoManager.queryLike(ImportService.FOLDER_FIELD_NAME, property.getPath(), BookArchive.class);
        return archivesList.stream().collect(Collectors.toMap(BookArchive::getContentId, Function.identity()));
    }

    public static void calculateVolumesAndChaptersCount(List<String> changedSeriePaths) {
        long time = System.currentTimeMillis();

        List<BookSerie> series = changedSeriePaths.stream()
                .map(seriePath -> daoManager.queryLike(ImportService.FOLDER_FIELD_NAME, seriePath, BookSerie.class))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(BookSerie.class::cast)
                .collect(Collectors.toList());

        ProgressBar cliProgressBar = new ProgressBarBuilder()
                .setTaskName("Calculating:")
                .setInitialMax(series.size())
                .setStyle(JavaHelper.isWindows() ? ProgressBarStyle.ASCII : ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                .build();

        incrementTotal(series.size());

        series.forEach(serie -> {
            serie.setVolumesCount(daoManager.countArchivesForSerie(serie));
            serie.setChaptersCount(daoManager.countChaptersForSerie(serie));

            List<BookArchive> archives = daoManager.query("SERIE", String.valueOf(serie.getDbId()), BookArchive.class);
            archives.forEach(archive -> {
                archive.setVolumesCount(serie.getVolumesCount());
                daoManager.save(archive);
            });

            logFile(String.format("[%s:%s] volumes = %s, chapters = %s", serie.getTitle(), serie.getContentId(), serie.getVolumesCount(), serie.getChaptersCount()));
            daoManager.save(serie);
            incrementProgress();
            cliProgressBar.step();
        });

        cliProgressBar.close();
    }

    private static void log(String message) {
        logger.info(message);
        fileLogger.info(message);
    }

    private static void logFile(String message) {
        fileLogger.info(message);
    }

    private static void logWarn(String message) {
        logger.warn(message);
        fileLogger.warning(message);
    }

    private static void logError(String message) {
        logger.error(message);
        fileLogger.warning(message);
    }
}
