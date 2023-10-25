package com.atsumeru.web.repository;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.service.UserDatabaseDetailsService;
import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.model.database.User;
import kotlin.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HistoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(HistoryRepository.class.getSimpleName());
    private static final BooksDaoManager daoManager;
    private static final BooksDaoManager.HistoryDao historyDao;

    private static final BiPredicate<LibraryPresentation, IBaseBookItem> NOT_ARCHIVE_PREDICATE = (presentation, bookItem) ->
            presentation.isSeriesAndSinglesPresentation()
                    || presentation.isSeriesPresentation() && !bookItem.isSingle()
                    || presentation.isSinglesPresentation() && bookItem.isSingle();

    private static final BiPredicate<LibraryPresentation, IBaseBookItem> ARCHIVE_PREDICATE = (presentation, bookItem) -> presentation.isArchivesPresentation();

    public static List<IBaseBookItem> getBooksHistory(User user, LibraryPresentation libraryPresentation, int page, long limit) {
        long time = System.currentTimeMillis();
        boolean isSeries = libraryPresentation.isSeriesOrSinglesPresentation();

        // Запрашиваем записи Истории для текущего пользователя
        List<History> historyList = getHistoryForUser(user, page, limit, isSeries);

        // Собираем список хешей книг
        List<String> bookHashes = historyList.stream()
                .map(history -> history.getBookHash(isSeries))
                .collect(Collectors.toList());

        // Запрашиваем Книги из базы по списку хешей книг
        Map<String, IBaseBookItem> bookItems = daoManager.queryBooks(bookHashes, getHistoryClass(isSeries))
                .stream()
                .collect(Collectors.toMap(IBaseBookItem::getContentId, Function.identity()));

        List<IBaseBookItem> archivesList;
        if (isSeries) {
            // Собираем список ID Книг в базе данных
            List<Long> bookDbIds = bookItems.values()
                    .stream()
                    .map(IBaseBookItem::getDbId)
                    .collect(Collectors.toList());

            // Запрашиваем Архивы из базы по ID Серий
            archivesList = daoManager.queryArchivesForSeries(bookDbIds);
        } else {
            // Собираем Архивы из ранее подготовленной Map<String, IBaseBookItem>
            archivesList = new ArrayList<>(bookItems.values());
        }

        // Группируем Архивы по ID Серий
        Map<Long, List<IBaseBookItem>> archives = archivesList
                .stream()
                .collect(Collectors.groupingBy(IBaseBookItem::getSerieDbId, Collectors.mapping(Function.identity(), Collectors.toList())));

        // Собираем все Серии для записей Истории и создаем каждой серии Тома
        boolean includeFileInfo = UserDatabaseDetailsService.isIncludeFileInfoIntoResponse();
        List<IBaseBookItem> items = historyList.stream()
                .map(history -> history.getBookHash(isSeries))
                .filter(bookItems::containsKey)
                .map(bookItems::get)
                .filter(bookItem -> isSeries ? NOT_ARCHIVE_PREDICATE.test(libraryPresentation, bookItem) : ARCHIVE_PREDICATE.test(libraryPresentation, bookItem))
                .peek(bookItem -> BooksRepository.createVolumesWithHistoryForItem(user, archives.get(isSeries ? bookItem.getDbId() : bookItem.getSerieDbId()), bookItem, bookItem.getContentId(), isSeries, false, includeFileInfo))
                .collect(Collectors.toList());

        if (isSeries) {
            items = items.stream()
                    .filter(bookItem -> ArrayUtils.isNotEmpty(bookItem.getVolumes()))
                    .map(HistoryRepository::setVolumesHistoryAndMapToTriple)
                    .filter(triple -> triple.getSecond().get())
                    .sorted(Comparator.comparing(triple -> triple.getFirst().get(), Comparator.reverseOrder()))
                    .map(Triple::getThird)
                    .collect(Collectors.toList());
        }

        Set<String> disallowedGenres = user.getDisallowedGenres();
        Set<String> disallowedTags = user.getDisallowedTags();

        items = items.stream()
                .filter(item -> FilteredBooksRepository.isNotInSet(item.getGenres(), disallowedGenres))
                .filter(item -> FilteredBooksRepository.isNotInSet(item.getTags(), disallowedTags))
                .collect(Collectors.toList());

        logger.info("History querying finished. Took: " + (System.currentTimeMillis() - time) + "ms");
        return items;
    }

    private static List<History> getHistoryForUser(User user, int page, long limit, boolean isSeries) {
        return historyDao.query(
                "USER_ID",
                String.valueOf(user.getId()),
                "LAST_READ_AT",
                user.getAllowedContentTypes(),
                user.getAllowedCategoryIds(),
                false,
                (page - 1) * limit,
                limit,
                isSeries ? "SERIE_HASH" : "ARCHIVE_HASH"
        );
    }

    private static Triple<AtomicLong, AtomicBoolean, IBaseBookItem> setVolumesHistoryAndMapToTriple(IBaseBookItem bookItem) {
        AtomicLong lastRead = new AtomicLong();
        AtomicBoolean hasReadPages = new AtomicBoolean();
        AtomicBoolean firstUnreadCoverFound = new AtomicBoolean();

        bookItem.getVolumes().forEach(volume -> {
            History history = volume.getHistory();
            if (history != null) {
                lastRead.set(Math.max(lastRead.get(), history.getLastReadAt()));
                if (!hasReadPages.get()) {
                    hasReadPages.set(history.getCurrentPage() > 0);
                }
            }
            if (!firstUnreadCoverFound.get() && !volume.isRead()) {
                bookItem.setCover(volume.getId());
                BooksRepository.appendCoverTimestamp(bookItem);
                firstUnreadCoverFound.set(true);
            }
        });

        return new Triple<>(lastRead, hasReadPages, bookItem);
    }

    public static List<History> getBookHistory(User user, String bookOrArchiveHash) {
        return historyDao.queryByHashForUser(bookOrArchiveHash, String.valueOf(user.getId()), getHistoryClass(bookOrArchiveHash));
    }

    public static List<History> getBookHistory(String itemHash, Class<? extends IBaseBookItem> clazz) {
        return historyDao.queryByHash(itemHash, clazz);
    }

    public static void saveReadedPage(User user, String archiveHash, String chapterHash, int currentPage) {
        List<History> historyList = historyDao.queryByHashForUser(
                StringUtils.isNotEmpty(chapterHash) ? chapterHash : archiveHash,
                String.valueOf(user.getId()),
                StringUtils.isNotEmpty(chapterHash) ? BookChapter.class : BookArchive.class
        );

        History historyItem = ArrayUtils.isEmpty(historyList)
                ? createHistoryItem(user, archiveHash, chapterHash)
                : historyList.get(0);

        historyItem.setCurrentPage(currentPage);
        historyItem.setLastReadAt(System.currentTimeMillis());
        historyDao.save(historyItem);
    }

    private static History createHistoryItem(User user, String archiveHash, String chapterHash) {
        IBaseBookItem libraryItem;
        int pagesCount;

        if (StringUtils.isNotEmpty(chapterHash)) {
            BookChapter chapter = BooksRepository.getChapter(chapterHash);
            libraryItem = chapter.getArchive();
            pagesCount = chapter.getPagesCount();
        } else {
            libraryItem = BooksRepository.getBookDetails(user, archiveHash, false, false);
            pagesCount = libraryItem.getPagesCount();
        }

        return new History(user.getId(), libraryItem.getSerie(), archiveHash, chapterHash, pagesCount);
    }

    private static Class<? extends IBaseBookItem> getHistoryClass(String bookOrArchiveHash) {
        return daoManager.isExist(bookOrArchiveHash, BookSerie.class)
                ? BookSerie.class
                : BookArchive.class;
    }

    private static Class<? extends IBaseBookItem> getHistoryClass(boolean isSeries) {
        return isSeries ? BookSerie.class : BookArchive.class;
    }

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
        historyDao = daoManager.getHistoryDao();
    }
}
