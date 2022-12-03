package com.atsumeru.web.service;

import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.helper.JavaHelper;
import com.atsumeru.web.manager.ImageCache;
import com.atsumeru.web.model.book.BaseBook;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.covers.CoversCachingStatus;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.util.GUArray;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Component
public class CoversSaverService {
    private static final Logger logger = LoggerFactory.getLogger(CoversSaverService.class.getSimpleName());

    private static CoversSaverService INSTANCE;
    private static final AtomicBoolean isCachingActive = new AtomicBoolean(false);
    private static final AtomicLong cachingStartTime = new AtomicLong();
    private static final AtomicInteger progress = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();

    private final ExecutorService executorService;
    private final BooksDaoManager daoManager;

    public static CoversCachingStatus getStatus() {
        return new CoversCachingStatus(
                CoversSaverService.isCachingActive(),
                CoversSaverService.getRunningMs(),
                CoversSaverService.getProgress(),
                CoversSaverService.getTotal()
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    public void startService() {
        INSTANCE = new CoversSaverService();
        logger.info("Covers saver service started");
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

    private CoversSaverService() {
        executorService = Executors.newSingleThreadExecutor();
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
    }

    public static void saveNonExistentCoversIntoCache() {
        Thread thread = new Thread(() -> INSTANCE.saveNonExistentCoversIntoCache(
                INSTANCE.daoManager.queryAll(BookArchive.class, LibraryPresentation.ARCHIVES)
        ), CoversSaverService.class.getSimpleName());
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

        if (GUArray.isNotEmpty(booksWithoutCovers)) {
            ProgressBar cliProgressBar = new ProgressBarBuilder()
                    .setTaskName("Caching Covers:")
                    .setInitialMax(booksWithoutCovers.size())
                    .setStyle(JavaHelper.isWindows() ? ProgressBarStyle.ASCII : ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .build();

            booksWithoutCovers.stream()
                    .parallel()
                    .forEach(book -> {
                        String coverHash = Optional.ofNullable(book.getCover())
                                .filter(BooksRepository::isArchiveHash)
                                .orElse(book.getContentId());
                        try {
                            ImageCache.saveImageIntoCache(coverHash);
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
