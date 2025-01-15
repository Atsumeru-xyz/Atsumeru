package xyz.atsumeru.web.service;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.helper.JavaHelper;
import xyz.atsumeru.web.manager.ImageCache;
import xyz.atsumeru.web.model.book.BaseBook;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.covers.CoversCachingStatus;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.util.ArrayUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class CoversSaverService {
    private static final Logger logger = LoggerFactory.getLogger(CoversSaverService.class.getSimpleName());

    private static CoversSaverService INSTANCE;
    private static final AtomicBoolean isCachingActive = new AtomicBoolean(false);
    private static final AtomicLong cachingStartTime = new AtomicLong();
    private static final AtomicInteger progress = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();

    private final ExecutorService executorService;

    public static CoversCachingStatus getStatus() {
        return new CoversCachingStatus(
                CoversSaverService.isCachingActive(),
                CoversSaverService.getRunningMs(),
                CoversSaverService.getProgress(),
                CoversSaverService.getTotal()
        );
    }

    public static boolean isCachingActive() {
        return isCachingActive.get();
    }

    public static long getRunningMs() {
        return isCachingActive()
                ? System.currentTimeMillis() - cachingStartTime.get()
                : 0;
    }

    public static int getProgress() {
        return progress.get();
    }

    public static int getTotal() {
        return total.get();
    }

    public CoversSaverService() {
        executorService = Executors.newSingleThreadExecutor();
        logger.info("Covers saver service started!");
        INSTANCE = this;
    }

    public static void saveNonExistentCoversIntoCache() {
        Thread thread = new Thread(() -> INSTANCE.saveNonExistentCoversIntoCache(Beans.getBooksDaoManager().queryAll(BookArchive.class, LibraryPresentation.ARCHIVES)), CoversSaverService.class.getSimpleName());
        INSTANCE.executorService.submit(thread);
    }

    private void saveNonExistentCoversIntoCache(List<BaseBook> books) {
        isCachingActive.set(true);
        cachingStartTime.set(System.currentTimeMillis());

        List<BaseBook> booksWithoutCovers = books.stream()
                .peek(book -> book.setCover(Optional.ofNullable(book.getCover())
                        .filter(BooksRepository::isArchiveHash)
                        .orElse(book.getContentId())))
                .filter(book -> !ImageCache.isInCache(book.getCover(), ImageCache.ImageCacheType.THUMBNAIL))
                .collect(Collectors.toList());

        progress.set(0);
        total.set(booksWithoutCovers.size());

        if (ArrayUtils.isNotEmpty(booksWithoutCovers)) {
            ProgressBar cliProgressBar = new ProgressBarBuilder()
                    .setTaskName("Caching Covers:")
                    .setInitialMax(booksWithoutCovers.size())
                    .setStyle(JavaHelper.isWindows() ? ProgressBarStyle.ASCII : ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .build();

            booksWithoutCovers.stream()
                    .parallel()
                    .forEach(book -> {
                        logger.info("Saving: " + book.getFolder());
                        try {
                            ImageCache.saveImageIntoCache(
                                    Optional.ofNullable(book.getCover())
                                            .filter(BooksRepository::isArchiveHash)
                                            .orElse(book.getContentId())
                            );
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        progress.incrementAndGet();
                        cliProgressBar.step();
                    });

            cliProgressBar.close();
            System.out.println();
        }
        isCachingActive.set(false);
    }
}
