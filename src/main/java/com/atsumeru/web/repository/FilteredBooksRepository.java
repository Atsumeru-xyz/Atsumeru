package com.atsumeru.web.repository;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.enums.*;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.model.database.User;
import com.atsumeru.web.model.filter.Filters;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUEnum;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import com.atsumeru.web.util.comparator.NaturalStringComparator;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class FilteredBooksRepository {
    private static final Comparator<IBaseBookItem> TITLE_COMPARATOR = (item1, item2) -> NaturalStringComparator.compareStrings(item1.getTitle(), item2.getTitle());

    public static List<IBaseBookItem> getFilteredList(User user, ContentType contentType, String category, LibraryPresentation libraryPresentation, String search,
                                                      Sort sort, boolean ascending, MultiValueMap<String, String> filtersMap, int page, int limit, boolean withVolumesAndHistory, boolean withChapters) {
        Status status = GUEnum.valueOfOrNull(Status.class, filtersMap.getFirst("status"));
        TranslationStatus translationStatus = GUEnum.valueOfOrNull(TranslationStatus.class, filtersMap.getFirst("translation_status"));
        Censorship censorship = GUEnum.valueOfOrNull(Censorship.class, filtersMap.getFirst("censorship"));
        Color color = GUEnum.valueOfOrNull(Color.class, filtersMap.getFirst("color"));
        AgeRating ageRating = GUEnum.valueOfOrNull(AgeRating.class, filtersMap.getFirst("age_rating"));

        LogicalMode authorsMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("authors_mode"));
        LogicalMode artistsMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("artists_mode"));
        LogicalMode publishersMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("publishers_mode"));
        LogicalMode translatorsMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("translators_mode"));
        LogicalMode genresMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("genres_mode"));
        LogicalMode tagsMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("tags_mode"));
        LogicalMode countriesMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("countries_mode"));
        LogicalMode languagesMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("languages_mode"));
        LogicalMode eventsMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("event_mode"));
        LogicalMode charactersMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("characters_mode"));
        LogicalMode parodiesMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("parodies_mode"));
        LogicalMode circlesMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("circles_mode"));
        LogicalMode magazinesMode = GUEnum.valueOf(LogicalMode.class, filtersMap.getFirst("magazines_mode"));

        return getFilteredList(user, contentType, category, libraryPresentation, search, sort, ascending, status, translationStatus, censorship, color, ageRating,
                GUArray.splitString(filtersMap.getFirst("authors"), ","), authorsMode,
                GUArray.splitString(filtersMap.getFirst("artists"), ","), artistsMode,
                GUArray.splitString(filtersMap.getFirst("publishers"), ","), publishersMode,
                GUArray.splitString(filtersMap.getFirst("translators"), ","), translatorsMode,
                GUArray.splitString(filtersMap.getFirst("genres"), ","), genresMode,
                GUArray.splitString(filtersMap.getFirst("tags"), ","), tagsMode,
                GUArray.splitString(filtersMap.getFirst("countries"), ","), countriesMode,
                GUArray.splitString(filtersMap.getFirst("languages"), ","), languagesMode,
                GUArray.splitString(filtersMap.getFirst("events"), ","), eventsMode,
                GUArray.splitString(filtersMap.getFirst("characters"), ","), charactersMode,
                GUArray.splitString(filtersMap.getFirst("parodies"), ","), parodiesMode,
                GUArray.splitString(filtersMap.getFirst("circles"), ","), circlesMode,
                GUArray.splitString(filtersMap.getFirst("magazines"), ","), magazinesMode,
                GUArray.splitString(filtersMap.getFirst("years"), ","), page, limit,
                withVolumesAndHistory, withChapters);
    }

    public static List<IBaseBookItem> getFilteredList(User user, ContentType contentType, String category, LibraryPresentation libraryPresentation, String search, Sort sort,
                                                      boolean ascending, Status status, TranslationStatus translationStatus, Censorship censorship, Color color, AgeRating ageRating,
                                                      List<String> authors, LogicalMode authorsMode, List<String> artists, LogicalMode artistsMode,
                                                      List<String> publishers, LogicalMode publishersMode, List<String> translators, LogicalMode translatorsMode,
                                                      List<String> genres, LogicalMode genresMode, List<String> tags, LogicalMode tagsMode,
                                                      List<String> countries, LogicalMode countriesMode, List<String> languages, LogicalMode languagesMode,
                                                      List<String> events, LogicalMode eventsMode, List<String> characters, LogicalMode charactersMode,
                                                      List<String> parodies, LogicalMode parodiesMode, List<String> circles, LogicalMode circlesMode,
                                                      List<String> magazines, LogicalMode magazinesMode, List<String> years,
                                                      int page, int limit, boolean withVolumesAndHistory, boolean withChapters) {
        List<IBaseBookItem> list = BooksRepository.getBooks(user, libraryPresentation, 1, Integer.MAX_VALUE, false, false);

        Map<String, Category> allowedCategoriesMap = user.getAllowedCategoriesMap();
        List<ContentType> allowedContentTypes = allowedCategoriesMap.values().stream()
                .map(Category::getContentType)
                .filter(GUString::isNotEmpty)
                .map(type -> GUEnum.valueOfOrNull(ContentType.class, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> allowedCategories = allowedCategoriesMap.values().stream()
                .map(Category::getCategoryId)
                .map(CategoryRepository::createDbIdForCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> disallowedGenres = user.getDisallowedGenres();
        Set<String> disallowedTags = user.getDisallowedTags();

        List<String> genreIds = genresToIds(genres);
        List<IBaseBookItem> filteredList = list.stream().filter(it -> {
            if (GUString.isNotEmpty(search)) {
                if (!(GUString.containsIgnoreCase(it.getTitle(), search)
                        || GUString.containsIgnoreCase(it.getAltTitle(), search)
                        || GUString.containsIgnoreCase(it.getJapTitle(), search)
                        || GUString.containsIgnoreCase(it.getKorTitle(), search)
                        || GUString.containsIgnoreCase(it.getAuthors(), search)
                        || GUString.containsIgnoreCase(it.getArtists(), search)
                        || GUString.containsIgnoreCase(it.getTranslators(), search)
                        || GUString.containsIgnoreCase(it.getPublisher(), search)
                        // TODO: localized genres support
                        || GUString.containsIgnoreCase(it.getGenres(), search)
                        || GUString.containsIgnoreCase(it.getTags(), search)
                        || GUString.containsIgnoreCase(it.getYear(), search)
                        || GUString.containsIgnoreCase(it.getCountry(), search)
                        || GUString.containsIgnoreCase(it.getLanguage(), search)
                        || GUString.containsIgnoreCase(it.getEvent(), search)
                        || GUString.containsIgnoreCase(it.getCharacters(), search)
                        || GUString.containsIgnoreCase(it.getParodies(), search)
                        || GUString.containsIgnoreCase(it.getCircles(), search)
                        || GUString.containsIgnoreCase(it.getMagazines(), search)
                        || GUString.containsIgnoreCase(it.getDescription(), search))) {
                    return false;
                }
            }

            if (GUString.isNotEmpty(it.getCategories()) && GUString.isNotEmpty(category)) {
                if (!it.getCategories().contains(category)) {
                    return false;
                }
            }

            if (contentType != null && it.getContentType() != contentType
                    || status != null && it.getStatus() != status
                    || translationStatus != null && it.getTranslationStatus() != translationStatus
                    || plotType != null && it.getPlotType() != plotType
                    || censorship != null && it.getCensorship() != censorship
                    || color != null && it.getColor() != color
                    || ageRating != null && it.getAgeRating() != ageRating) {
                return false;
            }

            if (!itemHasFiltersEntry(authors, GUArray.splitString(it.getAuthors(), ","), authorsMode)
                    || !itemHasFiltersEntry(artists, GUArray.splitString(it.getArtists(), ","), artistsMode)
                    || !itemHasFiltersEntry(publishers, GUArray.splitString(it.getPublisher(), ","), publishersMode)
                    || !itemHasFiltersEntry(translators, GUArray.splitString(it.getTranslators(), ","), translatorsMode)
                    || !itemHasFiltersEntry(years, GUArray.splitString(it.getYear(), ","), LogicalMode.OR)
                    || !itemHasFiltersEntry(genreIds, GUArray.splitString(it.getGenres(), ","), genresMode)
                    || !itemHasFiltersEntry(tags, GUArray.splitString(it.getTags(), ","), tagsMode)
                    || !itemHasFiltersEntry(countries, GUArray.splitString(it.getCountry(), ","), countriesMode)
                    || !itemHasFiltersEntry(languages, GUArray.splitString(it.getLanguage(), ","), languagesMode)
                    || !itemHasFiltersEntry(events, GUArray.splitString(it.getEvent(), ","), eventsMode)
                    || !itemHasFiltersEntry(characters, GUArray.splitString(it.getCharacters(), ","), charactersMode)
                    || !itemHasFiltersEntry(parodies, GUArray.splitString(it.getParodies(), ","), parodiesMode)
                    || !itemHasFiltersEntry(circles, GUArray.splitString(it.getCircles(), ","), circlesMode)
                    || !itemHasFiltersEntry(magazines, GUArray.splitString(it.getMagazines(), ","), magazinesMode)) {
                return false;
            }

            // TODO: filter by user created categories
            if (GUArray.isNotEmpty(allowedCategoriesMap) && !allowedContentTypes.contains(it.getContentType())) {
                return false;
            }

            if (GUArray.isNotEmpty(allowedCategories) && GUString.isNotEmpty(it.getCategories())) {
                List<String> categories = GUArray.splitString(it.getCategories(), ",");
                boolean noneMatch = categories.stream().noneMatch(allowedCategories::contains);
                return !noneMatch;
            }

            return true;
        })
                .filter(item -> isNotInSet(item.getGenres(), disallowedGenres))
                .filter(item -> isNotInSet(item.getTags(), disallowedTags))
                .sorted(getSortComparator(sort))
                .collect(Collectors.toList());

        if (sort != null && !ascending) {
            Collections.reverse(filteredList);
        }

        int subListFromIndex = (page - 1) * limit;
        int subListToIndex = page * limit;

        if (subListFromIndex > filteredList.size()) {
            return new ArrayList<>();
        }
        if (subListToIndex > filteredList.size()) {
            subListToIndex = filteredList.size();
        }

        List<IBaseBookItem> cuttedFilteredList;
        if (sort == Sort.LAST_READ) {
            BooksRepository.loadVolumesAndChaptersInfo(user, libraryPresentation, filteredList, withVolumesAndHistory, withChapters, false);
            filteredList.sort(getLastReadComparator());
            cuttedFilteredList = filteredList.subList(subListFromIndex, subListToIndex);
        } else {
            cuttedFilteredList = filteredList.subList(subListFromIndex, subListToIndex);
            BooksRepository.loadVolumesAndChaptersInfo(user, libraryPresentation, cuttedFilteredList, withVolumesAndHistory, withChapters, false);
        }

        return cuttedFilteredList;
    }

    private static List<String> genresToIds(List<String> genres) {
        List<String> genreIds = new ArrayList<>();
        if (GUArray.isNotEmpty(genres)) {
            genres.forEach(it -> {
                Genre genre = Genre.getGenreFromString(it);
                if (genre != null) {
                    genreIds.add(String.valueOf(genre.ordinal()));
                }
            });
        }

        return genreIds;
    }

    private static boolean itemHasFiltersEntry(List<String> filters, List<String> values, LogicalMode logicalMode) {
        if (GUArray.isNotEmpty(filters)) {
            if (GUArray.isEmpty(values)) {
                for (String filter : filters) {
                    if (!isNegativeFilter(filter)) {
                        // Positive filter. Condition not met
                        return false;
                    }
                }

                // All filters is negative, so, condition is met
                return true;
            }

            values = values.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            boolean containsAny = false;
            for (String filter : filters) {
                filter = filter.toLowerCase();

                // Check is filter is negative
                boolean isNegativeFilter = isNegativeFilter(filter);
                if (isNegativeFilter) {
                    // Remove first symbol that is always "-" for negative filter
                    filter = filter.substring(1);

                    // Checking negative filter
                    // Negative filters always in AND mode!
                    if (values.contains(filter)) {
                        return false;
                    }
                } else {
                    // Checking positive filter
                    if (logicalMode == LogicalMode.AND && !values.contains(filter)) {
                        return false;
                    } else if /* OR mode */ (logicalMode == LogicalMode.OR && values.contains(filter)) {
                        containsAny = true;
                        break;
                    }
                }
            }

            return logicalMode != LogicalMode.OR || containsAny;
        }
        return true;
    }

    private static boolean isNegativeFilter(String filter) {
        return GUString.isNotEmpty(filter) && filter.startsWith("-");
    }

    private static Comparator<IBaseBookItem> getSortComparator(Sort sort) {
        if (sort == null) {
            return Comparator.comparingLong(IBaseBookItem::getDbId);
        }

        switch (sort) {
            case TITLE:
                return TITLE_COMPARATOR;
            case YEAR:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(item1.getYear(), item2.getYear())).thenComparing(TITLE_COMPARATOR);
            case COUNTRY:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(item1.getCountry(), item2.getCountry())).thenComparing(TITLE_COMPARATOR);
            case LANGUAGE:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(item1.getLanguage(), item2.getLanguage())).thenComparing(TITLE_COMPARATOR);
            case PUBLISHER:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(item1.getPublisher(), item2.getPublisher())).thenComparing(TITLE_COMPARATOR);
            case PARODY:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(
                        GUArray.safeGetString(GUArray.splitString(item1.getParodies()), 0, ""),
                        GUArray.safeGetString(GUArray.splitString(item2.getParodies()), 0, "")
                )).thenComparing(TITLE_COMPARATOR);
            case VOLUMES_COUNT:
                return Comparator.comparingLong(IBaseBookItem::getVolumesCount).thenComparing(TITLE_COMPARATOR);
            case CHAPTERS_COUNT:
                return Comparator.comparingLong(IBaseBookItem::getChaptersCount).thenComparing(TITLE_COMPARATOR);
            case SCORE:
                return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(item1.getScore(), item2.getScore())).thenComparing(TITLE_COMPARATOR);
            case CREATED_AT:
                return Comparator.comparingLong(IBaseBookItem::getCreatedAt).thenComparing(TITLE_COMPARATOR);
            case UPDATED_AT:
                return Comparator.comparingLong(IBaseBookItem::getUpdatedAt).thenComparing(TITLE_COMPARATOR);
            case POPULARITY:
                return Comparator.comparingInt(IBaseBookItem::getRating).thenComparing(TITLE_COMPARATOR);
            default:
                return Comparator.comparingLong(IBaseBookItem::getDbId);
        }
    }

    public static Comparator<IBaseBookItem> getParodyComparator() {
        return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(
                GUArray.safeGetString(GUArray.splitString(item1.getParodies()), 0, ""),
                GUArray.safeGetString(GUArray.splitString(item2.getParodies()), 0, "")
        )).thenComparing(TITLE_COMPARATOR);
    }

    public static Comparator<IBaseBookItem> getLastReadComparator() {
        return Comparator.comparingLong(FilteredBooksRepository::getLastReadForBook).thenComparing(TITLE_COMPARATOR);
    }

    private static long getLastReadForBook(IBaseBookItem bookItem) {
        return Optional.ofNullable(bookItem.getVolumes())
                .filter(GUArray::isNotEmpty)
                .map(list -> list.stream()
                        .map(VolumeItem::getHistory)
                        .filter(Objects::nonNull).max(Comparator.comparingLong(History::getLastReadAt))
                        .map(History::getLastReadAt)
                        .orElse(0L)
                )
                .orElse(0L);
    }

    public static List<Filters> getFiltersList(ContentType contentType, String category, LibraryPresentation libraryPresentation) {
        List<IBaseBookItem> items = BooksDatabaseRepository.getInstance().getDaoManager()
                .queryAll(libraryPresentation.getDbClassForPresentation(), libraryPresentation);

        items = items.stream()
                .filter(item -> contentType != null ? item.getContentType() == contentType && !item.isRemoved() : !item.isRemoved())
                .filter(item -> GUString.isEmpty(category) || Optional.ofNullable(item.getCategories())
                        .map(categories -> categories.contains(category))
                        .orElse(false))
                .collect(Collectors.toList());

        List<Filters> filters = new ArrayList<>();

        List<String> sort = new ArrayList<>();
        Set<String> contentTypes = new TreeSet<>();
        Set<String> statuses = new TreeSet<>();
        Set<String> translationStatuses = new TreeSet<>();
        Set<String> plotTypes = new TreeSet<>();
        Set<String> censorships = new TreeSet<>();
        Set<String> colors = new TreeSet<>();
        Set<String> ageRatings = new TreeSet<>();
        Set<String> authors = new TreeSet<>();
        Set<String> artists = new TreeSet<>();
        Set<String> publishers = new TreeSet<>();
        Set<String> translators = new TreeSet<>();
        Set<String> genres = new TreeSet<>();
        Set<String> tags = new TreeSet<>();
        Set<String> years = new TreeSet<>();
        Set<String> countries = new TreeSet<>();
        Set<String> languages = new TreeSet<>();
        Set<String> events = new TreeSet<>();
        Set<String> characters = new TreeSet<>();
        Set<String> parodies = new TreeSet<>();
        Set<String> circles = new TreeSet<>();
        Set<String> magazines = new TreeSet<>();

        fillListWithEnumNames(sort, Sort.class);

        items.forEach(item -> {
            if (contentType == null) {
                fillSetWithEnumName(contentTypes, item.getContentType());
            }
            fillSetWithEnumName(statuses, item.getStatus());
            fillSetWithEnumName(translationStatuses, item.getTranslationStatus());
            fillSetWithEnumName(censorships, item.getCensorship());
            fillSetWithEnumName(colors, item.getColor());
            fillSetWithEnumName(ageRatings, item.getAgeRating());

            GUArray.fillSetWithStringAsArray(authors, item.getAuthors());
            GUArray.fillSetWithStringAsArray(artists, item.getArtists());
            GUArray.fillSetWithList(publishers, Collections.singletonList(item.getPublisher()));
            GUArray.fillSetWithStringAsArray(translators, item.getTranslators());
            GUArray.fillSetWithStringAsArray(genres, item.getGenres());
            GUArray.fillSetWithStringAsArray(tags, item.getTags());
            GUArray.fillSetWithList(years, Collections.singletonList(item.getYear()));
            GUArray.fillSetWithList(countries, Collections.singletonList(item.getCountry()));
            GUArray.fillSetWithStringAsArray(languages, item.getLanguage());
            GUArray.fillSetWithList(events, Collections.singletonList(item.getEvent()));
            GUArray.fillSetWithStringAsArray(characters, item.getCharacters());
            GUArray.fillSetWithStringAsArray(series, item.getSeries());
            GUArray.fillSetWithStringAsArray(parodies, item.getParodies());
            GUArray.fillSetWithStringAsArray(circles, item.getCircles());
            GUArray.fillSetWithStringAsArray(magazines, item.getMagazines());
        });

        createFiltersFromSets(filters, sort, contentTypes, statuses, translationStatuses, plotTypes, censorships, colors, ageRatings, authors, artists,
                publishers, translators, genres, tags, years, countries, languages, events, characters, series, parodies, circles, magazines);

        return filters;
    }

    private static void createFiltersFromSets(List<Filters> filters, List<String> sort, Set<String> contentTypes, Set<String> statuses,
                                              Set<String> translationStatuses, Set<String> plotTypes, Set<String> censorships, Set<String> colors,
                                              Set<String> ageRatings, Set<String> authors, Set<String> artists, Set<String> publishers,
                                              Set<String> translators, Set<String> genres, Set<String> tags, Set<String> years,
                                              Set<String> countries, Set<String> languages, Set<String> events, Set<String> characters,
                                              Set<String> series, Set<String> parodies, Set<String> circles, Set<String> magazines) {
        // Single mode filters without AND modifier
        safeAddCollectionIntoFiltersList(filters, sort, "sort", false, true);
        safeAddCollectionIntoFiltersList(filters, contentTypes, "type", false, true);
        safeAddCollectionIntoFiltersList(filters, statuses, "status", false, true);
        safeAddCollectionIntoFiltersList(filters, translationStatuses, "translation_status", false, true);
        safeAddCollectionIntoFiltersList(filters, plotTypes, "plot_type", false, true);
        safeAddCollectionIntoFiltersList(filters, censorships, "censorship", false, true);
        safeAddCollectionIntoFiltersList(filters, colors, "color", false, true);
        safeAddCollectionIntoFiltersList(filters, ageRatings, "age_rating", false, true);

        // Multiple mode filters with AND modifier
        safeAddCollectionIntoFiltersList(filters, authors, "authors", true, false);
        safeAddCollectionIntoFiltersList(filters, artists, "artists", true, false);
        safeAddCollectionIntoFiltersList(filters, publishers, "publishers", true, false);
        safeAddCollectionIntoFiltersList(filters, translators, "translators", true, false);
        safeAddCollectionIntoFiltersList(filters, genres, "genres", true, false);
        safeAddCollectionIntoFiltersList(filters, tags, "tags", true, false);
        safeAddCollectionIntoFiltersList(filters, years, "years", true, false);
        safeAddCollectionIntoFiltersList(filters, countries, "countries", true, false);
        safeAddCollectionIntoFiltersList(filters, languages, "languages", true, false);
        safeAddCollectionIntoFiltersList(filters, events, "events", true, false);
        safeAddCollectionIntoFiltersList(filters, characters, "characters", true, false);
        safeAddCollectionIntoFiltersList(filters, parodies, "parodies", true, false);
        safeAddCollectionIntoFiltersList(filters, circles, "circles", true, false);
        safeAddCollectionIntoFiltersList(filters, magazines, "magazines", true, false);
    }

    private static void safeAddCollectionIntoFiltersList(List<Filters> filters, Collection<String> collection, String id, boolean hasAndMode, boolean singleMode) {
        if (GUArray.isNotEmpty(collection)) {
            filters.add(Filters.create(id, Localizr.toLocale("filters." + id), hasAndMode, singleMode, new ArrayList<>(collection)));
        }
    }

    private static void fillListWithEnumNames(List<String> set, Class<? extends Enum<?>> e) {
        String[] names = GUEnum.getNames(e);
        for (String name : names) {
            fillListWithEnumName(set, name);
        }
    }

    private static void fillListWithEnumName(List<String> list, Enum<?> enumValue) {
        fillListWithEnumName(list, enumValue.name());
    }

    private static void fillListWithEnumName(List<String> set, String enumName) {
        if (GUString.isEmpty(enumName)) {
            return;
        }
        set.add(enumName);
    }

    private static void fillSetWithEnumNames(Set<String> set, Class<? extends Enum<?>> e) {
        String[] names = GUEnum.getNames(e);
        for (String name : names) {
            fillSetWithEnumName(set, name);
        }
    }

    private static void fillSetWithEnumName(Set<String> set, Enum<?> enumValue) {
        fillSetWithEnumName(set, enumValue.name());
    }

    private static void fillSetWithEnumName(Set<String> set, String enumName) {
        if (GUString.isEmpty(enumName)) {
            return;
        }
        set.add(enumName);
    }

    public static boolean isNotInSet(String itemsStr, Set<String> set) {
        return Optional.ofNullable(itemsStr)
                .filter(GUString::isNotEmpty)
                .map(items -> GUArray.splitString(items, ","))
                .map(Collection::stream)
                .map(stream -> stream.map(String::toLowerCase))
                .map(stream -> stream.noneMatch(set::contains))
                .orElse(true);
    }
}
