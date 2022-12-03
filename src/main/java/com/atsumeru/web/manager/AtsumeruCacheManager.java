package com.atsumeru.web.manager;

import com.djvu2image.DjVuBook;
import com.atsumeru.web.util.GUFile;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Configurable
@Configuration
@EnableCaching(proxyTargetClass = true)
public class AtsumeruCacheManager extends CachingConfigurerSupport implements ApplicationContextAware {
    public static final Cache<File, PDDocument> PDF_CACHE = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener((file, pdf, cause) -> GUFile.closeQuietly((Closeable) pdf))
            .build();

    public static final Cache<File, DjVuBook> DJVU_CACHE = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    private static ApplicationContext context;

    public static void evictAll() {
        context.getBean(AtsumeruCacheManager.class).evictAllInternal();
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public void evictAllInternal() {
        CacheManager cacheManager = context.getBean(CacheManager.class);
        for (String name : cacheManager.getCacheNames()) {
            try {
                cacheManager.getCache(name).clear();
            } catch (Exception ignored) {
            }
        }
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("books"),
                new ConcurrentMapCache("books_by_bound_service"),
                new ConcurrentMapCache("filters"),
                new ConcurrentMapCache("hub-updates"),
                new ConcurrentMapCache("history")
        ));
        return cacheManager;
    }
}
