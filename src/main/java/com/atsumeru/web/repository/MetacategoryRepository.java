package com.atsumeru.web.repository;

import com.atsumeru.web.model.book.BaseBook;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.helper.ValuesMapper;
import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.enums.Sort;
import com.atsumeru.web.model.category.Metacategory;
import com.atsumeru.web.model.database.User;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MetacategoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(MetacategoryRepository.class.getSimpleName());

    @Getter
    private static Set<Metacategory> metacategories;
    private static final Map<String, List<Metacategory>> metacategoryEntries = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Order(11)
    public void init() {
        long time = System.currentTimeMillis();
        createMetacategories();
        logger.info("Metacategories indexed. Took " + (System.currentTimeMillis() - time) + "ms");
    }

    public static List<Metacategory> getEntries(String metacategoryId) {
        return metacategoryEntries.get(metacategoryId.toLowerCase())
                .stream()
                .sorted((metacategory1, metacategory2) -> AlphanumComparator.compareStrings(metacategory1.getName(), metacategory2.getName()))
                .collect(Collectors.toList());
    }

    public static void reIndex() {
        metacategoryEntries.clear();
        createMetacategories();
    }

    private static void createMetacategories() {
        metacategories = new TreeSet<>((item1, item2) -> AlphanumComparator.compareStrings(item1.getName(), item2.getName()));
        createMetacategory(metacategories, "PUBLISHER", "publishers");
        createMetacategory(metacategories, "AUTHORS");
        createMetacategory(metacategories, "ARTISTS");
        createMetacategory(metacategories, "TRANSLATORS");
        createMetacategory(metacategories, "GENRES");
        createMetacategory(metacategories, "TAGS");
        createMetacategory(metacategories, "YEAR", "years");
        createMetacategory(metacategories, "CENSORSHIP");
        createMetacategory(metacategories, "PARODIES");
        createMetacategory(metacategories, "CIRCLES");
        createMetacategory(metacategories, "MAGAZINES");
        createMetacategory(metacategories, "CHARACTERS");
        createMetacategory(metacategories, "COLOR");
        createMetacategory(metacategories, "COUNTRY", "countries");
        createMetacategory(metacategories, "LANGUAGE", "languages");
        createMetacategory(metacategories, "EVENT", "events");
    }

    private static void createMetacategory(Set<Metacategory> set, String columnName) {
        createMetacategory(set, columnName, columnName);
    }

    private static void createMetacategory(Set<Metacategory> set, String columnName, String metacategoryId) {
        if (BooksDatabaseRepository.getInstance().getDaoManager().countNotEmpty(columnName.toUpperCase(), BookSerie.class) > 0) {
            List<Metacategory> entries = getEntriesFromDb(metacategoryId);
            set.add(new Metacategory(
                    metacategoryId.toLowerCase(),
                    Localizr.toLocale("metacategory." + columnName.toLowerCase()),
                    entries.size()
            ));

            metacategoryEntries.putIfAbsent(metacategoryId.toLowerCase(), entries);
        }
    }

    private static List<Metacategory> getEntriesFromDb(String metacategoryId) {
        Map<String, Long> tags = BooksDatabaseRepository.getInstance().getDaoManager()
                .queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES)
                .stream()
                .map(BookSerie.class::cast)
                .filter(BaseBook::notRemoved)
                .map(item -> ValuesMapper.getMangaValue(item, metacategoryId.toLowerCase(), false))
                .map(StringUtils::trimSquareBrackets)
                .map(ArrayUtils::splitString)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return tags.keySet()
                .stream()
                .map(key -> new Metacategory(key, key, tags.get(key)))
                .collect(Collectors.toList());
    }

    public static List<IBaseBookItem> getFilteredList(User user, String metacategoryId, String filter, int page,
                                                      int limit, boolean withVolumesAndHistory, boolean withChapters) {
        MultiValueMap<String, String> filtersMap = new LinkedMultiValueMap<>();
        filtersMap.add(metacategoryId.toLowerCase(), filter);
        return FilteredBooksRepository.getFilteredList(user, null, null, LibraryPresentation.SERIES_AND_SINGLES,
                null, Sort.CREATED_AT, true, filtersMap, page, limit, withVolumesAndHistory, withChapters);
    }
}
