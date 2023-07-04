package com.atsumeru.web.controller.rest.book;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.helper.ArchiveHelper;
import com.atsumeru.web.helper.FilesHelper;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.FilteredBooksRepository;
import com.atsumeru.web.repository.UserDatabaseRepository;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.enums.*;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.manager.ImageCache;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.filter.Filters;
import com.atsumeru.web.util.GUArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RestController
@RequestMapping("/api/v1/books")
public class BooksApiController {
    @Autowired
    UserDatabaseRepository userService;

    //*****************************//
    //*         Books             *//
    //*****************************//
    @GetMapping("")
    @Cacheable(value = "books", key="#request.userPrincipal.name.concat('-')" +
            ".concat(\"\" + #contentType).concat('-')" +
            ".concat(#category).concat('-')" +
            ".concat(#libraryPresentation).concat('-')" +
            ".concat(#search).concat('-')" +
            ".concat(#sort).concat('-')" +
            ".concat(#ascending).concat('-')" +
            ".concat(#page).concat('-')" +
            ".concat(#limit).concat('-')" +
            ".concat(#withVolumesAndHistory).concat('-')" +
            ".concat(#withChapters).concat('-')" +
            ".concat(#getAll).concat('-')")
    public List<IBaseBookItem> getBooks(HttpServletRequest request,
                                        @RequestParam(value = "type", defaultValue = "") ContentType contentType,
                                        @RequestParam(value = "category", defaultValue = "") String category,
                                        @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation,
                                        @RequestParam(value = "search", defaultValue = "") String search,
                                        @RequestParam(value = "sort", defaultValue = "created_at") Sort sort,
                                        @RequestParam(value = "asc", defaultValue = "false") boolean ascending,
                                        @RequestParam(value = "page", defaultValue = "1") int page,
                                        @RequestParam(value = "limit", defaultValue = "30") int limit,
                                        @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                        @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters,
                                        @RequestParam(value = "all", defaultValue = "false") boolean getAll) {
        if (GUString.isEmpty(search)) {
            return BooksRepository.getBooks(
                    userService.getUserFromRequest(request),
                    libraryPresentation,
                    CategoryRepository.getContentTypeForCategory(category, contentType),
                    CategoryRepository.createDbIdForCategoryId(category),
                    sort,
                    ascending,
                    page,
                    limit,
                    Settings.isAllowListLoadingWithVolumes() && withVolumesAndHistory,
                    Settings.isAllowListLoadingWithChapters() && withChapters,
                    getAll);
        } else {
            return getFilteredBooks(request,
                    contentType,
                    category,
                    libraryPresentation,
                    search,
                    sort,
                    ascending,
                    new LinkedMultiValueMap<>(),
                    page,
                    limit,
                    withVolumesAndHistory,
                    withChapters);
        }
    }

    //*****************************//
    //*         Filters           *//
    //*****************************//
    @GetMapping("/filters")
    @Cacheable(value = "filters", key="#request.userPrincipal.name.concat('-')" +
            ".concat(\"\" + #contentType).concat('-')" +
            ".concat(#category).concat('-')" +
            ".concat(#libraryPresentation).concat('-')")
    public List<Filters> getFiltersList(HttpServletRequest request,
                                        @RequestParam(value = "type", defaultValue = "", required = false) ContentType contentType,
                                        @RequestParam(value = "category", defaultValue = "", required = false) String category,
                                        @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation) {
        return FilteredBooksRepository.getFiltersList(
                CategoryRepository.getContentTypeForCategory(category, contentType),
                CategoryRepository.createDbIdForCategoryId(category),
                libraryPresentation
        );
    }

    @PostMapping("/filtered")
    public List<IBaseBookItem> getFilteredBooks(HttpServletRequest request,
                                                @RequestParam(value = "type", defaultValue = "", required = false) ContentType contentType,
                                                @RequestParam(value = "category", defaultValue = "") String category,
                                                @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation,
                                                @RequestParam(value = "search", defaultValue = "") String search,
                                                @RequestParam(value = "sort", defaultValue = "") Sort sort,
                                                @RequestParam(value = "asc", defaultValue = "true") boolean ascending,
                                                @RequestBody MultiValueMap<String, String> formData,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "limit", defaultValue = "30") int limit,
                                                @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                                @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return FilteredBooksRepository.getFilteredList(
                userService.getUserFromRequest(request),
                CategoryRepository.getContentTypeForCategory(category, contentType),
                CategoryRepository.createDbIdForCategoryId(category),
                libraryPresentation,
                search,
                sort,
                ascending,
                formData,
                page,
                limit,
                Settings.isAllowListLoadingWithVolumes() && withVolumesAndHistory,
                Settings.isAllowListLoadingWithChapters() && withChapters);
    }

    @GetMapping("/filtered")
    public List<IBaseBookItem> getFilteredBooks(HttpServletRequest request,
                                                @RequestParam(value = "type", defaultValue = "", required = false) ContentType contentType,
                                                @RequestParam(value = "category", defaultValue = "") String category,
                                                @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation,
                                                @RequestParam(value = "search", defaultValue = "") String search,
                                                @RequestParam(value = "sort", defaultValue = "") Sort sort,
                                                @RequestParam(value = "asc", defaultValue = "true") boolean ascending,
                                                // Single filters
                                                @RequestParam(value = "status", defaultValue = "") Status status,
                                                @RequestParam(value = "translation_status", defaultValue = "") TranslationStatus translationStatus,
                                                @RequestParam(value = "plot_type", defaultValue = "") PlotType plotType,
                                                @RequestParam(value = "censorship", defaultValue = "") Censorship censorship,
                                                @RequestParam(value = "color", defaultValue = "") Color color,
                                                @RequestParam(value = "age_rating", defaultValue = "") AgeRating ageRating,
                                                // Multiple filters
                                                @RequestParam(value = "authors", defaultValue = "") List<String> authors,
                                                @RequestParam(value = "authors_mode", defaultValue = "and") LogicalMode authorsMode,
                                                @RequestParam(value = "artists", defaultValue = "") List<String> artists,
                                                @RequestParam(value = "artists_mode", defaultValue = "and") LogicalMode artistsMode,
                                                @RequestParam(value = "publishers", defaultValue = "") List<String> publishers,
                                                @RequestParam(value = "publishers_mode", defaultValue = "and") LogicalMode publishersMode,
                                                @RequestParam(value = "translators", defaultValue = "") List<String> translators,
                                                @RequestParam(value = "translators_mode", defaultValue = "and") LogicalMode translatorsMode,
                                                @RequestParam(value = "genres", defaultValue = "") List<String> genres,
                                                @RequestParam(value = "genres_mode", defaultValue = "and") LogicalMode genresMode,
                                                @RequestParam(value = "tags", defaultValue = "") List<String> tags,
                                                @RequestParam(value = "tags_mode", defaultValue = "and") LogicalMode tagsMode,
                                                @RequestParam(value = "countries", defaultValue = "") List<String> countries,
                                                @RequestParam(value = "countries_mode", defaultValue = "and") LogicalMode countriesMode,
                                                @RequestParam(value = "languages", defaultValue = "") List<String> languages,
                                                @RequestParam(value = "languages_mode", defaultValue = "and") LogicalMode languagesMode,
                                                @RequestParam(value = "events", defaultValue = "") List<String> events,
                                                @RequestParam(value = "event_mode", defaultValue = "and") LogicalMode eventsMode,
                                                @RequestParam(value = "characters", defaultValue = "") List<String> characters,
                                                @RequestParam(value = "characters_mode", defaultValue = "and") LogicalMode charactersMode,
                                                @RequestParam(value = "series", defaultValue = "") List<String> series,
                                                @RequestParam(value = "series_mode", defaultValue = "and") LogicalMode seriesMode,
                                                @RequestParam(value = "parodies", defaultValue = "") List<String> parodies,
                                                @RequestParam(value = "parodies_mode", defaultValue = "and") LogicalMode parodiesMode,
                                                @RequestParam(value = "circles", defaultValue = "") List<String> circles,
                                                @RequestParam(value = "circles_mode", defaultValue = "and") LogicalMode circlesMode,
                                                @RequestParam(value = "magazines", defaultValue = "") List<String> magazines,
                                                @RequestParam(value = "magazines_mode", defaultValue = "and") LogicalMode magazinesMode,
                                                // Years always in OR mode
                                                @RequestParam(value = "years", defaultValue = "") List<String> years,
                                                // Other params
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "limit", defaultValue = "30") int limit,
                                                @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                                @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return FilteredBooksRepository.getFilteredList(
                userService.getUserFromRequest(request),
                CategoryRepository.getContentTypeForCategory(category, contentType),
                CategoryRepository.createDbIdForCategoryId(category),
                libraryPresentation, search, sort,ascending, status, translationStatus, plotType, censorship,
                color, ageRating, authors, authorsMode, artists, artistsMode, publishers, publishersMode, translators, translatorsMode,
                genres, genresMode, tags, tagsMode, countries, countriesMode, languages, languagesMode, events, eventsMode, characters,
                charactersMode, series, seriesMode, parodies, parodiesMode, circles, circlesMode, magazines, magazinesMode, years, page, limit,
                Settings.isAllowListLoadingWithVolumes() && withVolumesAndHistory,
                Settings.isAllowListLoadingWithChapters() && withChapters
        );
    }

    //*****************************//
    //*         Details          *//
    //*****************************//
    @GetMapping("/{book_hash}")
    public IBaseBookItem getBookDetails(HttpServletRequest request, @PathVariable(value = "book_hash") String bookHash,
                                        @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                        @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return BooksRepository.getBookDetails(userService.getUserFromRequest(request), bookHash, withVolumesAndHistory, withChapters);
    }

    @GetMapping(value = {
            "/{serie_hash}/serie",
            "/{serie_hash}/franchise"
    })
    public List<IBaseBookItem> getSerieFranchiseBooks(HttpServletRequest request, @PathVariable(value = "serie_hash") String serieHash) {
        return BooksRepository.getSerieFranchiseBooks(userService.getUserFromRequest(request), serieHash);
    }

    //*****************************//
    //*         Volumes           *//
    //*****************************//
    @GetMapping(value = {
            "/{book_hash}/volumes",
            "/{book_hash}/issues"
    })
    public List<VolumeItem> getVolumes(HttpServletRequest request,
                                       @PathVariable(value = "book_hash") String bookHash,
                                       @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return GUArray.getNotNullList(BooksRepository.getBookDetails(userService.getUserFromRequest(request), bookHash, true, withChapters).getVolumes());
    }

    @GetMapping(value = {
            "/volumes/{archive_hash}",
            "/issues/{archive_hash}",
            "/{book_hash}/volumes/{archive_hash}",
            "/{book_hash}/issues/{archive_hash}"
    })
    public VolumeItem getVolume(HttpServletRequest request,
                                @PathVariable(value = "book_hash", required = false) String bookHash,
                                @PathVariable(value = "archive_hash") String archiveHash,
                                @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return getVolumes(request, archiveHash, withChapters).get(0);
    }

    //*****************************//
    //*         Chapters          *//
    //*****************************//
    @GetMapping(value = "/{book_hash}/chapters")
    public List<BookChapter> getChapters(HttpServletRequest request, @PathVariable(value = "book_hash") String bookHash) {
        return BooksRepository.getBookDetails(userService.getUserFromRequest(request), bookHash, true, true)
                .getVolumes()
                .stream()
                .filter(Objects::nonNull)
                .map(VolumeItem::getChapters)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @GetMapping(value = {
            "/volumes/{archive_hash}/chapters",
            "/issues/{archive_hash}/chapters",
            "/{book_hash}/volumes/{archive_hash}/chapters",
            "/{book_hash}/issues/{archive_hash}/chapters"
    })
    public List<BookChapter> getVolumeChapters(HttpServletRequest request,
                                               @PathVariable(value = "book_hash", required = false) String bookHash,
                                               @PathVariable(value = "archive_hash") String archiveHash) {
        return getChapters(request, archiveHash);
    }

    @GetMapping(value = {
            "/chapters/{chapter_hash}",
            "/{book_hash}/chapters/{chapter_hash}"
    })
    public BookChapter getChapter(@PathVariable(value = "book_hash", required = false) String bookHash,
                                  @PathVariable(value = "chapter_hash") String chapterHash) {
        return BooksRepository.getChapter(chapterHash);
    }

    //*****************************//
    //*         Pages             *//
    //*****************************//
    @GetMapping(value = {
            "/{archive_or_chapter_hash}/page/{page}",
            "/{book_hash}/volumes/{archive_hash}/page/{page}",
            "/{book_hash}/issues/{archive_hash}/page/{page}",
            "/{book_hash}/chapter/{chapter_hash}/page/{page}",
            "/{book_hash}/volumes/{archive_hash}/chapter/{chapter_hash}/page/{page}"
    })
    public void getPage(HttpServletResponse response,
                        @PathVariable(value = "book_hash", required = false) String bookHash,
                        @PathVariable(value = "archive_or_chapter_hash", required = false) String archiveOrChapterHash,
                        @PathVariable(value = "archive_hash", required = false) String archiveHash,
                        @PathVariable(value = "chapter_hash", required = false) String chapterHash,
                        @PathVariable(value = "page") int page,
                        @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) {
        ArchiveHelper.getBookPage(
                response,
                !BooksRepository.isChapterHash(archiveOrChapterHash)
                        ? archiveOrChapterHash
                        : archiveHash,
                BooksRepository.isChapterHash(archiveOrChapterHash)
                        ? archiveOrChapterHash
                        : chapterHash,
                page,
                convertImage
        );
    }

    //*****************************//
    //*         Downloads         *//
    //*****************************//
    @GetMapping("/download/{archive_hash}")
    public void downloadBook(HttpServletResponse response, @PathVariable(value = "archive_hash") String archiveHash) throws IOException {
        FilesHelper.downloadFile(response, SecurityContextHolder.getContext().getAuthentication(), archiveHash);
    }

    //*****************************//
    //*         Deletion          *//
    //*****************************//
    @DeleteMapping("/delete/{book_hash}")
    public ResponseEntity<AtsumeruMessage> deleteArchive(HttpServletRequest request, @PathVariable(value = "book_hash") String bookHash) {
        BooksRepository.deleteBook(userService.getUserFromRequest(request), bookHash);
        AtsumeruCacheManager.evictAll();
        return RestHelper.createResponseMessage("Book deleted", HttpStatus.OK);
    }

    //*****************************//
    //*         Covers            *//
    //*****************************//
    @GetMapping(value = "/cover/{image_hash}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBookCover(HttpServletResponse response,
                                             @PathVariable(value = "image_hash") String imageHash,
                                             @RequestParam(value = "type", defaultValue = "original") ImageCache.ImageCacheType imageCacheType,
                                             @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) {
        return FilesHelper.getCover(response, imageHash, imageCacheType, convertImage);
    }
}