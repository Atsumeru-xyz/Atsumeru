package com.atsumeru.web.repository;

import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.enums.Sort;
import com.atsumeru.web.exception.ChapterNotFoundException;
import com.atsumeru.web.exception.NoReadableFoundException;
import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.model.database.User;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.service.UserDatabaseDetailsService;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import kotlin.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BooksRepository {
    private static final BooksDaoManager daoManager;

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
    }

    /* ***************************************** */
    /*                Book List                  */
    /* ***************************************** */
    public static List<IBaseBookItem> getBooks(User user, LibraryPresentation libraryPresentation, long page, long limit,
                                               boolean withVolumesAndHistory, boolean withChapters) {
        return getBooks(user, libraryPresentation, null, null, Sort.TITLE, true, page, limit, withVolumesAndHistory, withChapters, false);
    }

    public static List<IBaseBookItem> getBooks(User user, LibraryPresentation libraryPresentation, ContentType contentType, String category, Sort sort,
                                               boolean ascendingOrder, long page, long limit, boolean withVolumesAndHistory, boolean withChapters, boolean getAll) {
        boolean isGetAll = getAll || sort == Sort.LAST_READ || sort == Sort.PARODY;

        long realOffset = isGetAll ? 0 : (page - 1) * limit;
        long realLimit = isGetAll ? Integer.MAX_VALUE : limit;

        Set<String> disallowedGenres = user.getDisallowedGenres();
        Set<String> disallowedTags = user.getDisallowedTags();

        List<IBaseBookItem> list = daoManager.query(
                        getOrderByColumnNameForSort(sort),
                        ascendingOrder,
                        contentType,
                        category,
                        user.getAllowedContentTypes(),
                        user.getAllowedCategoryIds(),
                        libraryPresentation,
                        libraryPresentation.getDbClassForPresentation()
                ).stream()
                .map(IBaseBookItem.class::cast)
                .filter(item -> FilteredBooksRepository.isNotInSet(item.getGenres(), disallowedGenres))
                .filter(item -> FilteredBooksRepository.isNotInSet(item.getTags(), disallowedTags))
                .skip(realOffset)
                .limit(realLimit)
                .peek(BooksRepository::postGetBook)
                .collect(Collectors.toList());

        loadVolumesAndChaptersInfo(user, libraryPresentation, list, withVolumesAndHistory, withChapters, getAll);

        if (sort == Sort.LAST_READ || sort == Sort.PARODY || sort == Sort.SERIE) {
            switch (sort) {
                case LAST_READ:
                    list.sort(FilteredBooksRepository.getLastReadComparator());
                    break;
                case PARODY:
                    list.sort(FilteredBooksRepository.getParodyComparator());
                    break;
                case SERIE:
                    list.sort(FilteredBooksRepository.getSerieComparator());
                    break;
            }
            return list.stream()
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return list;
    }

    private static void postGetBook(IBaseBookItem item) {
        appendCoverTimestamp(item);
        if (item instanceof BookSerie) {
            ((BookSerie) item).prepareBoundServices();
        }
    }

    /* ***************************************** */
    /*               Book Details                */
    /* ***************************************** */
    public static IBaseBookItem getBookDetails(String itemHash) {
        return getBookDetails(null, itemHash, false, false);
    }

    public static IBaseBookItem getBookDetails(User user, String itemHash, boolean withVolumes, boolean withChapters) {
        long time = System.currentTimeMillis();
        boolean isSerie = isSeriesHash(itemHash);

        List<IBaseBookItem> mangaList = getItemsList(itemHash, isSerie);
        IBaseBookItem serieItem = getSerieItem(mangaList, itemHash, isSerie);

        if (GUArray.isEmpty(mangaList) || serieItem == null) {
            throw new NoReadableFoundException();
        }

        postGetBook(serieItem);
        if (withVolumes || withChapters) {
            createVolumesWithHistoryForItem(user, mangaList, serieItem, itemHash, isSerie, withChapters, UserDatabaseDetailsService.isIncludeFileInfoIntoResponse());
        }

        // TODO: log into file
//        logger.info(String.format("Readable details (%s, %s) query time: %sms", serieItem.getTitle(), itemHash, (System.currentTimeMillis() - time)));
        return serieItem;
    }

    public static List<IBaseBookItem> getSerieFranchiseBooks(User user, String itemHash) {
        IBaseBookItem serieItem = getSerieItem(null, itemHash, true);
        Set<String> series = GUArray.splitString(serieItem.getSeries(), ",")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (GUArray.isNotEmpty(series)) {
            List<IBaseBookItem> list = daoManager.queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES)
                    .stream()
                    .map(BookSerie.class::cast)
                    .filter(bookSerie -> GUArray.splitString(bookSerie.getSeries(), ",")
                            .stream()
                            .map(String::toLowerCase)
                            .anyMatch(serie -> GUArray.isInSet(series, serie)))
                    .sorted(getPlotTypeComparator())
                    .collect(Collectors.toList());

            loadVolumesAndChaptersInfo(user, LibraryPresentation.SERIES_AND_SINGLES, list, true, false, false);
            return list;
        }

        return new ArrayList<>();
    }

    public static Comparator<IBaseBookItem> getPlotTypeComparator() {
        return Comparator.comparingInt((IBaseBookItem item) -> item.getPlotType().getOrder())
                .thenComparing((item1, item2) -> AlphanumComparator.compareStrings(item1.getYear(), item2.getYear()))
                .thenComparing(FilteredBooksRepository.TITLE_COMPARATOR);
    }

    /* ***************************************** */
    /*                  Chapter                  */
    /* ***************************************** */
    public static BookChapter getChapter(String itemHash) {
        List<BookChapter> chapters = daoManager.query(itemHash, BookChapter.class);
        if (GUArray.isNotEmpty(chapters)) {
            return chapters.get(0);
        }
        throw new ChapterNotFoundException();
    }

    /* ***************************************** */
    /*               Book Deletion               */
    /* ***************************************** */
    public static void deleteBook(User user, String bookHash) {
        IBaseBookItem bookItem = getBookDetails(user, bookHash, false, false);
        bookItem.setRemoved(true);
        daoManager.save(bookItem);
    }

    /* ***************************************** */
    /*              Helper Methods               */
    /* ***************************************** */
    private static List<IBaseBookItem> getItemsList(String itemHash, boolean isSerieOrSingle) {
        return isSerieOrSingle
                ? getArchivesForSerie(itemHash)
                : daoManager.query(itemHash, BookArchive.class);
    }

    @Nullable
    private static IBaseBookItem getSerieItem(List<IBaseBookItem> mangaList, String itemHash, boolean isSerie) {
        if (!isSerie && GUArray.isNotEmpty(mangaList)) {
            return mangaList.get(0);
        } else {
            List<IBaseBookItem> serieList = daoManager.query(itemHash, BookSerie.class);
            if (GUArray.isNotEmpty(serieList)) {
                return serieList.get(0);
            }
        }
        return null;
    }

    public static void appendCoverTimestamp(IBaseBookItem baseLibraryItem) {
        baseLibraryItem.setCover(baseLibraryItem.getCover() + "?t=" + baseLibraryItem.getUpdatedAt());
    }

    public static void loadVolumesAndChaptersInfo(User user, LibraryPresentation libraryPresentation, List<IBaseBookItem> list,
                                                  boolean withVolumesAndHistory, boolean withChapters, boolean getAll) {
        if (withVolumesAndHistory || withChapters) {
            boolean isSerieOrSingle = libraryPresentation.isSeriesOrSinglesPresentation();
            List<History> historyList = getHistoryForItem(user, list, isSerieOrSingle);
            List<IBaseBookItem> archives = getArchivesForItems(list, isSerieOrSingle, getAll);
            Map<Long, List<IBaseBookItem>> archivesMap = archivesToMap(archives, isSerieOrSingle);
            Map<String, List<BookChapter>> chaptersMap = !Settings.isDisableChapters() && !getAll
                    ? getChaptersToMapForArchives(archives, withChapters, isSerieOrSingle, getAll)
                    : new HashMap<>();

            list.forEach(item -> createVolumesWithHistoryForItem(
                    archivesMap.get(item.getDbId()),
                    item,
                    chaptersMap.get(item.getContentId()),
                    historyList,
                    isSerieOrSingle,
                    withChapters,
                    UserDatabaseDetailsService.isIncludeFileInfoIntoResponse()
            ));
        }
    }

    private static List<History> getHistoryForItem(User user, List<IBaseBookItem> list, boolean isSerieOrSingle) {
        return daoManager.getHistoryDao().queryByHashesForUser(
                list.stream()
                        .map(IBaseBookItem::getContentId)
                        .collect(Collectors.toList()),
                String.valueOf(user.getId()),
                isSerieOrSingle ? BookSerie.class : BookArchive.class
        );
    }

    private static List<IBaseBookItem> getArchivesForItems(List<IBaseBookItem> list, boolean isSerieOrSingle, boolean getAll) {
        if (!isSerieOrSingle) {
            return list;
        }
        return getAll
                ? daoManager.queryAll(BookArchive.class, LibraryPresentation.ARCHIVES)
                : daoManager.queryArchivesForSeries(
                list.stream()
                        .map(IBaseBookItem::getDbId)
                        .collect(Collectors.toList())
        );
    }

    private static Map<Long, List<IBaseBookItem>> archivesToMap(List<IBaseBookItem> archives, boolean isSerieOrSingle) {
        // <SerieID in db (Long), List<Archive>>
        Map<Long, List<IBaseBookItem>> map = new HashMap<>();
        for (IBaseBookItem archive : archives) {
            map.computeIfAbsent(isSerieOrSingle ? archive.getSerieDbId() : archive.getDbId(), k -> new ArrayList<>()).add(archive);
        }

        return map;
    }

    private static Map<String, List<BookChapter>> getChaptersToMapForArchives(List<IBaseBookItem> archives, boolean loadChapters, boolean isSerieOrSingle, boolean getAll) {
        // <SerieID hash (String), List<Chapter>>
        Map<String, List<BookChapter>> map = new HashMap<>();
        if (loadChapters) {
            List<BookChapter> chapters = getAll
                    ? daoManager.queryAll(BookChapter.class)
                    : daoManager.queryChaptersForArchives(
                    archives.stream()
                            .map(IBaseBookItem::getDbId)
                            .collect(Collectors.toList())
            );

            for (BookChapter bookChapter : chapters) {
                map.computeIfAbsent(isSerieOrSingle ? bookChapter.getSerieId() : bookChapter.getArchiveId(), k -> new ArrayList<>()).add(bookChapter);
            }
        }
        return map;
    }

    private static void changeCoverToFirstUnreadArchive(IBaseBookItem item) {
        if (!isSeriesHash(item.getCover())) {
            item.getVolumes()
                    .stream()
                    .filter(volumeItem -> !volumeItem.isRead())
                    .findFirst()
                    .ifPresent(volumeItem -> item.setCover(volumeItem.getId()));
        }
    }

    public static void createVolumesWithHistoryForItem(User user, List<IBaseBookItem> contentList, IBaseBookItem baseItem,
                                                       String itemHash, boolean isSerie, boolean withChapters, boolean includeFileInfo) {
        if (GUArray.isEmpty(contentList)) {
            return;
        }

        List<BookChapter> chapters = !Settings.isDisableChapters()
                ? daoManager.queryChapters(
                contentList.stream()
                        .filter(item -> withChapters)
                        .map(IBaseBookItem::getContentId)
                        .collect(Collectors.toList())
        )
                : new ArrayList<>();

        List<History> historyList = daoManager.getHistoryDao().queryByHashForUser(
                itemHash,
                String.valueOf(user.getId()),
                isSerie ? BookSerie.class : BookArchive.class
        );

        createVolumesWithHistoryForItem(contentList, baseItem, chapters, historyList, isSerie, withChapters, includeFileInfo);
    }


    private static void createVolumesWithHistoryForItem(List<IBaseBookItem> contentList, IBaseBookItem baseItem, List<BookChapter> chapters,
                                                        List<History> historyList, boolean isSerie, boolean withChapters, boolean includeFileInfo) {
        if (GUArray.isEmpty(contentList)) {
            return;
        }

        boolean isSerieObject = baseItem instanceof BookSerie;
        boolean isSingleMode = isSerieObject && baseItem.isSingle();

        List<VolumeItem> volumes = contentList.stream()
                .map(content -> new Pair<>(
                                content,
                                historyList.stream()
                                        .filter(history -> GUString.equalsIgnoreCase(content.getContentId(), history.getArchiveHash()))
                                        .findFirst()
                                        .orElse(null)
                        )
                )
                .map(pair -> pair.getFirst().createVolumeItem(chapters, pair.getSecond(), historyList, isSingleMode, !isSerie, withChapters, includeFileInfo))
                .collect(Collectors.toList());

        if (!isSingleMode) {
            volumes.sort(Comparator.comparing(VolumeItem::getVolume, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        baseItem.addVolumes(volumes);
        changeCoverToFirstUnreadArchive(baseItem);
    }

    public static List<IBaseBookItem> getArchivesForSerie(String serieHash) {
        List<IBaseBookItem> serieList = daoManager.query(serieHash, BookSerie.class);
        if (GUArray.isNotEmpty(serieList)) {
            IBaseBookItem baseItem = serieList.get(0);
            return daoManager.query("SERIE", String.valueOf(baseItem.getDbId()), BookArchive.class);
        }
        return null;
    }

    public static boolean isSeriesHash(String itemHash) {
        return GUString.isNotEmpty(itemHash) && itemHash.startsWith(Constants.SERIE_HASH_TAG);
    }

    public static boolean isArchiveHash(@Nullable String itemHash) {
        return GUString.isNotEmpty(itemHash) && itemHash.startsWith(Constants.ARCHIVE_HASH_TAG);
    }

    public static boolean isChapterHash(@Nullable String itemHash) {
        return GUString.isNotEmpty(itemHash) && !itemHash.startsWith(Constants.ARCHIVE_HASH_TAG);
    }

    private static String getOrderByColumnNameForSort(Sort sort) {
        if (sort == null || sort == Sort.LAST_READ || sort == Sort.PARODY) {
            return Sort.CREATED_AT.name();
        } else if (sort == Sort.POPULARITY) {
            return "RATING";
        }
        return sort.name();
    }
}
