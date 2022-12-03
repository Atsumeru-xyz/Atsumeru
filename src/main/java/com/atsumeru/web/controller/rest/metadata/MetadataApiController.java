package com.atsumeru.web.controller.rest.metadata;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.MetacategoryRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.metadata.MetadataUpdateStatus;
import com.atsumeru.web.service.MetadataUpdateService;
import com.atsumeru.web.util.GUArray;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(MetadataApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('METADATA_UPDATER')")
public class MetadataApiController {
    public static final String ROOT_ENDPOINT = "/api/v1/metadata";
    private static final String STATUS_ENDPOINT = "/status";

    private final BooksDaoManager daoManager = BooksDatabaseRepository.getInstance().getDaoManager();

    @GetMapping(STATUS_ENDPOINT)
    public MetadataUpdateStatus getStatus() {
        return new MetadataUpdateStatus(
                MetadataUpdateService.isUpdateActive(),
                MetadataUpdateService.getRunningMs(),
                MetadataUpdateService.getProgress(),
                MetadataUpdateService.getTotal()
        );
    }

    @PatchMapping({"/update", "/edit"})
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> updateMetadata(@RequestBody BookSerie bookSerie,
                                                          @RequestParam(value = "serie_only", defaultValue = "false") boolean serieOnly,
                                                          @RequestParam(value = "into_archives", defaultValue = "false") boolean insertIntoArchives,
                                                          @RequestParam(value = "into_db_only", defaultValue = "false") boolean insertIntoDBOnly) {
        List<BookSerie> seriesInDb = daoManager.query(bookSerie.getContentId(), BookSerie.class);
        List<BookArchive> archivesInDb = daoManager.query(bookSerie.getContentId(), BookArchive.class);
        if (GUArray.isNotEmpty(seriesInDb)) {
            BookSerie serieInDb = seriesInDb.get(0);
            serieInDb.copyFromBaseBook(bookSerie);
            serieInDb.fromBoundServicesToIds();
            daoManager.save(serieInDb);

            CategoryRepository.reLoadCategories();
            MetacategoryRepository.reIndex();

            if (serieOnly) {
                return RestHelper.createResponseMessage("Serie updated in database", HttpStatus.OK);
            }

            return RestHelper.createResponseMessage(
                    MetadataUpdateService.getInstance().startUpdateForSerie(serieInDb, insertIntoArchives, insertIntoDBOnly)
                            ? "Updated in database. Started local updating..."
                            : "Unable to start metadata update. One of the update services already running...",
                    HttpStatus.OK);
        } else if (GUArray.isNotEmpty(archivesInDb) && !serieOnly) {
            BookArchive archiveInDb = archivesInDb.get(0);
            archiveInDb.copyFromBaseBook(bookSerie);
            daoManager.save(archiveInDb);

            CategoryRepository.reLoadCategories();
            MetacategoryRepository.reIndex();

            return RestHelper.createResponseMessage(
                    MetadataUpdateService.getInstance().startUpdateForArchive(archiveInDb, insertIntoArchives, insertIntoDBOnly)
                            ? "Updated in database. Started local updating..."
                            : "Unable to start metadata update. One of the update services already running...",
                    HttpStatus.OK);
        }

        return RestHelper.createResponseMessage("Candidate for metadata injecting not found in database", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/create_unique_hashes")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> createUniqueIds(@RequestParam(value = "into_archives", defaultValue = "false") boolean insertIntoArchives,
                                                           @RequestParam(value = "into_db_only", defaultValue = "false") boolean insertIntoDBOnly,
                                                           @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return RestHelper.createResponseMessage(
                MetadataUpdateService.getInstance().startCreatingUniqueIds(insertIntoArchives, insertIntoDBOnly, force)
                        ? "Started creating unique hashes..."
                        : "Unable to start creating unique hashes",
                HttpStatus.OK);
    }

    @GetMapping("/inject_all")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> injectAllFromDatabase() {
        return RestHelper.createResponseMessage(
                MetadataUpdateService.getInstance().startInjectAllFromDatabase()
                        ? "Started injecting metadata from database into archives..."
                        : "Unable to start injecting metadata from database into archives",
                HttpStatus.OK);
    }

    public static String getStatusEndpoint() {
        return ROOT_ENDPOINT + STATUS_ENDPOINT;
    }
}