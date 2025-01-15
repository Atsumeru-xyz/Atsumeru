package xyz.atsumeru.web.manager.cache;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@Configurable
@Configuration
@EnableCaching(proxyTargetClass = true)
public class AtsumeruCache implements ApplicationContextAware, CachingConfigurer {
    public static final String KEY_BOOKS = "books";
    public static final String KEY_BOOKS_BY_BOUND_SERVICE = "books_by_bound_service";
    public static final String KEY_FILTERS = "filters";
    public static final String KEY_HUB_UPDATES = "hub-updates";
    public static final String KEY_HISTORY = "history";

    private static ApplicationContext context;

    public static void evictAll() {
        context.getBean(AtsumeruCache.class).evictAllInternal();
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @CacheEvict(cacheNames = {KEY_BOOKS, KEY_BOOKS_BY_BOUND_SERVICE, KEY_FILTERS, KEY_HUB_UPDATES, KEY_HISTORY}, allEntries = true)
    public void evictAllInternal() {
        CacheManager cacheManager = context.getBean(CacheManager.class);
        for (String name : cacheManager.getCacheNames()) {
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(Cache::clear);
        }
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache(KEY_BOOKS),
                new ConcurrentMapCache(KEY_BOOKS_BY_BOUND_SERVICE),
                new ConcurrentMapCache(KEY_FILTERS),
                new ConcurrentMapCache(KEY_HUB_UPDATES),
                new ConcurrentMapCache(KEY_HISTORY)
        ));
        return cacheManager;
    }
}
