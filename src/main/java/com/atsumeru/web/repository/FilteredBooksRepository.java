package com.atsumeru.web.repository;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.model.database.User;
import com.atsumeru.web.model.filter.Filters;
import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.EnumUtils;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import com.atsumeru.web.util.comparator.NaturalStringComparator;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class FilteredBooksRepository {
    public static final Comparator<IBaseBookItem> TITLE_COMPARATOR = (item1, item2) -> NaturalStringComparator.compareStrings(item1.getTitle(), item2.getTitle());

    public static List<IBaseBookItem> getFilteredList(User user, ContentType contentType, String category, LibraryPresentation libraryPresentation, String search,
                                                      Sort sort, boolean ascending, MultiValueMap<String, String> filtersMap, int page, int limit, boolean withVolumesAndHistory, boolean withChapters) {
        Status status = EnumUtils.valueOfOrNull(Status.class, filtersMap.getFirst("status"));
        TranslationStatus translationStatus = EnumUtils.valueOfOrNull(TranslationStatus.class, filtersMap.getFirst("translation_status"));
        PlotType plotType = EnumUtils.valueOfOrNull(PlotType.class, filtersMap.getFirst("plot_type"));
        Censorship censorship = EnumUtils.valueOfOrNull(Censorship.class, filtersMap.getFirst("censorship"));
        Color color = EnumUtils.valueOfOrNull(Color.class, filtersMap.getFirst("color"));
        AgeRating ageRating = EnumUtils.valueOfOrNull(AgeRating.class, filtersMap.getFirst("age_rating"));

        LogicalMode authorsMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("authors_mode"));
        LogicalMode artistsMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("artists_mode"));
        LogicalMode publishersMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("publishers_mode"));
        LogicalMode translatorsMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("translators_mode"));
        LogicalMode genresMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("genres_mode"));
        LogicalMode tagsMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("tags_mode"));
        LogicalMode countriesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("countries_mode"));
        LogicalMode languagesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("languages_mode"));
        LogicalMode eventsMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("event_mode"));
        LogicalMode charactersMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("characters_mode"));
        LogicalMode seriesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("series_mode"));
        LogicalMode parodiesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("parodies_mode"));
        LogicalMode circlesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("circles_mode"));
        LogicalMode magazinesMode = EnumUtils.valueOf(LogicalMode.class, filtersMap.getFirst("magazines_mode"));

        return getFilteredList(user, contentType, category, libraryPresentation, search, sort, ascending, status, translationStatus, plotType, censorship, color, ageRating,
                ArrayUtils.splitString(filtersMap.getFirst("authors"), ","), authorsMode,
                ArrayUtils.splitString(filtersMap.getFirst("artists"), ","), artistsMode,
                ArrayUtils.splitString(filtersMap.getFirst("publishers"), ","), publishersMode,
                ArrayUtils.splitString(filtersMap.getFirst("translators"), ","), translatorsMode,
                ArrayUtils.splitString(filtersMap.getFirst("genres"), ","), genresMode,
                ArrayUtils.splitString(filtersMap.getFirst("tags"), ","), tagsMode,
                ArrayUtils.splitString(filtersMap.getFirst("countries"), ","), countriesMode,
                ArrayUtils.splitString(filtersMap.getFirst("languages"), ","), languagesMode,
                ArrayUtils.splitString(filtersMap.getFirst("events"), ","), eventsMode,
                ArrayUtils.splitString(filtersMap.getFirst("characters"), ","), charactersMode,
                ArrayUtils.splitString(filtersMap.getFirst("series"), ","), seriesMode,
                ArrayUtils.splitString(filtersMap.getFirst("parodies"), ","), parodiesMode,
                ArrayUtils.splitString(filtersMap.getFirst("circles"), ","), circlesMode,
                ArrayUtils.splitString(filtersMap.getFirst("magazines"), ","), magazinesMode,
                ArrayUtils.splitString(filtersMap.getFirst("years"), ","), page, limit,
                withVolumesAndHistory, withChapters);
    }

    public static List<IBaseBookItem> getFilteredList(User user, ContentType contentType, String category, LibraryPresentation libraryPresentation, String search,
                                                      Sort sort, boolean ascending, Status status, TranslationStatus translationStatus,
                                                      PlotType plotType, Censorship censorship, Color color, AgeRating ageRating,
                                                      List<String> authors, LogicalMode authorsMode, List<String> artists, LogicalMode artistsMode,
                                                      List<String> publishers, LogicalMode publishersMode, List<String> translators, LogicalMode translatorsMode,
                                                      List<String> genres, LogicalMode genresMode, List<String> tags, LogicalMode tagsMode,
                                                      List<String> countries, LogicalMode countriesMode, List<String> languages, LogicalMode languagesMode,
                                                      List<String> events, LogicalMode eventsMode, List<String> characters, LogicalMode charactersMode,
                                                      List<String> series, LogicalMode seriesMode, List<String> parodies, LogicalMode parodiesMode,
                                                      List<String> circles, LogicalMode circlesMode, List<String> magazines, LogicalMode magazinesMode, List<String> years,
                                                      int page, int limit, boolean withVolumesAndHistory, boolean withChapters) {
        List<IBaseBookItem> list = BooksRepository.getBooks(user, libraryPresentation, 1, Integer.MAX_VALUE, false, false);

        Map<String, Category> allowedCategoriesMap = user.getAllowedCategoriesMap();
        List<ContentType> allowedContentTypes = allowedCategoriesMap.values().stream()
                .map(Category::getContentType)
                .filter(StringUtils::isNotEmpty)
                .map(type -> EnumUtils.valueOfOrNull(ContentType.class, type))
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
            if (StringUtils.isNotEmpty(search)) {
                if (!(StringUtils.containsIgnoreCase(it.getTitle(), search)
                        || StringUtils.containsIgnoreCase(it.getAltTitle(), search)
                        || StringUtils.containsIgnoreCase(it.getJapTitle(), search)
                        || StringUtils.containsIgnoreCase(it.getKorTitle(), search)
                        || StringUtils.containsIgnoreCase(it.getAuthors(), search)
                        || StringUtils.containsIgnoreCase(it.getArtists(), search)
                        || StringUtils.containsIgnoreCase(it.getTranslators(), search)
                        || StringUtils.containsIgnoreCase(it.getPublisher(), search)
                        || StringUtils.containsIgnoreCase(it.getGenres(), search) // TODO: localized genres support
                        || StringUtils.containsIgnoreCase(it.getTags(), search)
                        || StringUtils.containsIgnoreCase(it.getYear(), search)
                        || StringUtils.containsIgnoreCase(it.getCountry(), search)
                        || StringUtils.containsIgnoreCase(it.getLanguage(), search)
                        || StringUtils.containsIgnoreCase(it.getEvent(), search)
                        || StringUtils.containsIgnoreCase(it.getCharacters(), search)
                        || StringUtils.containsIgnoreCase(it.getSeries(), search)
                        || StringUtils.containsIgnoreCase(it.getParodies(), search)
                        || StringUtils.containsIgnoreCase(it.getCircles(), search)
                        || StringUtils.containsIgnoreCase(it.getMagazines(), search)
                        || StringUtils.containsIgnoreCase(it.getDescription(), search))) {
                    return false;
                }
            }

            if (StringUtils.isNotEmpty(it.getCategories()) && StringUtils.isNotEmpty(category)) {
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

            if (!itemHasFiltersEntry(authors, ArrayUtils.splitString(it.getAuthors(), ","), authorsMode)
                    || !itemHasFiltersEntry(artists, ArrayUtils.splitString(it.getArtists(), ","), artistsMode)
                    || !itemHasFiltersEntry(publishers, ArrayUtils.splitString(it.getPublisher(), ","), publishersMode)
                    || !itemHasFiltersEntry(translators, ArrayUtils.splitString(it.getTranslators(), ","), translatorsMode)
                    || !itemHasFiltersEntry(years, ArrayUtils.splitString(it.getYear(), ","), LogicalMode.OR)
                    || !itemHasFiltersEntry(genreIds, ArrayUtils.splitString(it.getGenres(), ","), genresMode)
                    || !itemHasFiltersEntry(tags, ArrayUtils.splitString(it.getTags(), ","), tagsMode)
                    || !itemHasFiltersEntry(countries, ArrayUtils.splitString(it.getCountry(), ","), countriesMode)
                    || !itemHasFiltersEntry(languages, ArrayUtils.splitString(it.getLanguage(), ","), languagesMode)
                    || !itemHasFiltersEntry(events, ArrayUtils.splitString(it.getEvent(), ","), eventsMode)
                    || !itemHasFiltersEntry(characters, ArrayUtils.splitString(it.getCharacters(), ","), charactersMode)
                    || !itemHasFiltersEntry(series, ArrayUtils.splitString(it.getSeries(), ","), seriesMode)
                    || !itemHasFiltersEntry(parodies, ArrayUtils.splitString(it.getParodies(), ","), parodiesMode)
                    || !itemHasFiltersEntry(circles, ArrayUtils.splitString(it.getCircles(), ","), circlesMode)
                    || !itemHasFiltersEntry(magazines, ArrayUtils.splitString(it.getMagazines(), ","), magazinesMode)) {
                return false;
            }

            // TODO: filter by user created categories
            if (ArrayUtils.isNotEmpty(allowedCategoriesMap) && !allowedContentTypes.contains(it.getContentType())) {
                return false;
            }

            if (ArrayUtils.isNotEmpty(allowedCategories) && StringUtils.isNotEmpty(it.getCategories())) {
                List<String> categories = ArrayUtils.splitString(it.getCategories(), ",");
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
        if (ArrayUtils.isNotEmpty(genres)) {
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
        if (ArrayUtils.isNotEmpty(filters)) {
            if (ArrayUtils.isEmpty(values)) {
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
        return StringUtils.isNotEmpty(filter) && filter.startsWith("-");
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
            case SERIE:
                return getSerieComparator();
            case PARODY:
                return getParodyComparator();
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

    public static Comparator<IBaseBookItem> getSerieComparator() {
        return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(
                ArrayUtils.safeGetString(ArrayUtils.splitString(item1.getSeries()), 0, ""),
                ArrayUtils.safeGetString(ArrayUtils.splitString(item2.getSeries()), 0, "")
        )).thenComparing((item1, item2) -> AlphanumComparator.compareStrings(item1.getYear(), item2.getYear()))
                .thenComparing(TITLE_COMPARATOR);
    }

    public static Comparator<IBaseBookItem> getParodyComparator() {
        return ((Comparator<IBaseBookItem>) (item1, item2) -> AlphanumComparator.compareStrings(
                ArrayUtils.safeGetString(ArrayUtils.splitString(item1.getParodies()), 0, ""),
                ArrayUtils.safeGetString(ArrayUtils.splitString(item2.getParodies()), 0, "")
        )).thenComparing(TITLE_COMPARATOR);
    }

    public static Comparator<IBaseBookItem> getLastReadComparator() {
        return Comparator.comparingLong(FilteredBooksRepository::getLastReadForBook).thenComparing(TITLE_COMPARATOR);
    }

    private static long getLastReadForBook(IBaseBookItem bookItem) {
        return Optional.ofNullable(bookItem.getVolumes())
                .filter(ArrayUtils::isNotEmpty)
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
                .filter(item -> StringUtils.isEmpty(category) || Optional.ofNullable(item.getCategories())
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
        Set<String> series = new TreeSet<>();
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
            fillSetWithEnumName(plotTypes, item.getPlotType());
            fillSetWithEnumName(censorships, item.getCensorship());
            fillSetWithEnumName(colors, item.getColor());
            fillSetWithEnumName(ageRatings, item.getAgeRating());

            ArrayUtils.fillSetWithStringAsArray(authors, item.getAuthors());
            ArrayUtils.fillSetWithStringAsArray(artists, item.getArtists());
            ArrayUtils.fillSetWithList(publishers, Collections.singletonList(item.getPublisher()));
            ArrayUtils.fillSetWithStringAsArray(translators, item.getTranslators());
            ArrayUtils.fillSetWithStringAsArray(genres, item.getGenres());
            ArrayUtils.fillSetWithStringAsArray(tags, item.getTags());
            ArrayUtils.fillSetWithList(years, Collections.singletonList(item.getYear()));
            ArrayUtils.fillSetWithList(countries, Collections.singletonList(item.getCountry()));
            ArrayUtils.fillSetWithStringAsArray(languages, item.getLanguage());
            ArrayUtils.fillSetWithList(events, Collections.singletonList(item.getEvent()));
            ArrayUtils.fillSetWithStringAsArray(characters, item.getCharacters());
            ArrayUtils.fillSetWithStringAsArray(series, item.getSeries());
            ArrayUtils.fillSetWithStringAsArray(parodies, item.getParodies());
            ArrayUtils.fillSetWithStringAsArray(circles, item.getCircles());
            ArrayUtils.fillSetWithStringAsArray(magazines, item.getMagazines());
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
        safeAddCollectionIntoFiltersList(filters, series, "series", true, false);
        safeAddCollectionIntoFiltersList(filters, parodies, "parodies", true, false);
        safeAddCollectionIntoFiltersList(filters, circles, "circles", true, false);
        safeAddCollectionIntoFiltersList(filters, magazines, "magazines", true, false);
    }

    private static void safeAddCollectionIntoFiltersList(List<Filters> filters, Collection<String> collection, String id, boolean hasAndMode, boolean singleMode) {
        if (ArrayUtils.isNotEmpty(collection)) {
            filters.add(Filters.create(id, Localizr.toLocale("filters." + id), hasAndMode, singleMode, new ArrayList<>(collection)));
        }
    }

    private static void fillListWithEnumNames(List<String> set, Class<? extends Enum<?>> e) {
        String[] names = EnumUtils.getNames(e);
        for (String name : names) {
            fillListWithEnumName(set, name);
        }
    }

    private static void fillListWithEnumName(List<String> list, Enum<?> enumValue) {
        fillListWithEnumName(list, enumValue.name());
    }

    private static void fillListWithEnumName(List<String> set, String enumName) {
        if (StringUtils.isEmpty(enumName)) {
            return;
        }
        set.add(enumName);
    }

    private static void fillSetWithEnumNames(Set<String> set, Class<? extends Enum<?>> e) {
        String[] names = EnumUtils.getNames(e);
        for (String name : names) {
            fillSetWithEnumName(set, name);
        }
    }

    private static void fillSetWithEnumName(Set<String> set, Enum<?> enumValue) {
        fillSetWithEnumName(set, enumValue.name());
    }

    private static void fillSetWithEnumName(Set<String> set, String enumName) {
        if (StringUtils.isEmpty(enumName)) {
            return;
        }
        set.add(enumName);
    }

    public static boolean isNotInSet(String itemsStr, Set<String> set) {
        return Optional.ofNullable(itemsStr)
                .filter(StringUtils::isNotEmpty)
                .map(items -> ArrayUtils.splitString(items, ","))
                .map(Collection::stream)
                .map(stream -> stream.map(String::toLowerCase))
                .map(stream -> stream.noneMatch(set::contains))
                .orElse(true);
    }
}
