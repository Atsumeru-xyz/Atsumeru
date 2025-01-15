package xyz.atsumeru.web.controller.rest.metadata;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.metadata.MetadataUpdateStatus;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.repository.MetacategoryRepository;
import xyz.atsumeru.web.service.MetadataUpdateService;
import xyz.atsumeru.web.util.ArrayUtils;

import java.util.List;

@RestController
@RequestMapping(MetadataApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('METADATA_UPDATER')")
public class MetadataApiController {
    public static final String ROOT_ENDPOINT = "/api/v1/metadata";
    private static final String STATUS_ENDPOINT = "/status";

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
        List<BookSerie> seriesInDb = Beans.getBooksDaoManager().query(bookSerie.getContentId(), BookSerie.class);
        List<BookArchive> archivesInDb = Beans.getBooksDaoManager().query(bookSerie.getContentId(), BookArchive.class);
        if (ArrayUtils.isNotEmpty(seriesInDb)) {
            BookSerie serieInDb = seriesInDb.get(0);
            serieInDb.copyFromBaseBook(bookSerie);
            serieInDb.fromBoundServicesToIds();
            Beans.getBooksDaoManager().save(serieInDb);

            AtsumeruApplication.getContext().getBean(CategoryRepository.class).reLoadCategories();
            AtsumeruApplication.getContext().getBean(MetacategoryRepository.class).reIndex();

            if (serieOnly) {
                return RestHelper.createResponseMessage("Serie updated in database", HttpStatus.OK);
            }

            return RestHelper.createResponseMessage(
                    MetadataUpdateService.getInstance().startUpdateForSerie(serieInDb, insertIntoArchives, insertIntoDBOnly)
                            ? "Updated in database. Started local updating..."
                            : "Unable to start metadata update. One of the update services already running...",
                    HttpStatus.OK);
        } else if (ArrayUtils.isNotEmpty(archivesInDb) && !serieOnly) {
            BookArchive archiveInDb = archivesInDb.get(0);
            archiveInDb.copyFromBaseBook(bookSerie);
            Beans.getBooksDaoManager().save(archiveInDb);

            AtsumeruApplication.getContext().getBean(CategoryRepository.class).reLoadCategories();
            AtsumeruApplication.getContext().getBean(MetacategoryRepository.class).reIndex();

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