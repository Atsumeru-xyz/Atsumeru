package com.atsumeru.web.controller.rest.importer;

import com.atsumeru.web.model.book.BaseBook;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.service.ImportService;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.importer.Importer;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.importer.FolderProperty;
import com.atsumeru.web.model.importer.ImportStatus;
import com.atsumeru.web.properties.FoldersProperties;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping(ImporterApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('IMPORTER')")
public class ImporterApiController {
    protected static final String ROOT_ENDPOINT = "/api/v1/importer";
    private static final String STATUS_ENDPOINT = "/status";

    @GetMapping(STATUS_ENDPOINT)
    public ImportStatus getStatus() {
        return new ImportStatus(
                ImportService.isImportActive(),
                ImportService.getLastStartTime(),
                ImportService.getRunningMs(),
                Importer.getProgress(),
                Importer.getTotal()
        );
    }

    @GetMapping("/list")
    public List<FolderProperty> listFolders() {
        List<FolderProperty> properties = FoldersProperties.getFolderProperties();
        properties.forEach(property -> {
            property.setSeriesCount(countFolderItems(BookSerie.class, property.getPath(), LibraryPresentation.SERIES));
            property.setSinglesCount(countFolderItems(BookSerie.class, property.getPath(), LibraryPresentation.SINGLES));
            property.setArchivesCount(countFolderItems(BookArchive.class, property.getPath(), LibraryPresentation.ARCHIVES));
            property.setChaptersCount(countChapterItems(property.getPath()));
        });

        return properties;
    }

    @PostMapping("/add")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> addFolder(@RequestBody FolderProperty folderProperty) {
        File path = new File(folderProperty.getPath());
        boolean isDirectoryExistAndNotAdded = path.exists() && path.isDirectory()
                && !FoldersProperties.containsFolder(folderProperty.getPath());

        if (isDirectoryExistAndNotAdded) {
            folderProperty.createHash();
            FoldersProperties.addFolder(folderProperty);
            ImportService.add(folderProperty);
        }

        return RestHelper.createResponseMessage(
                isDirectoryExistAndNotAdded ? "Folder added" : "Folder not added",
                isDirectoryExistAndNotAdded ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    @DeleteMapping("/remove/{hash}")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> removeFolder(@PathVariable(value = "hash") String folderHash) {
        boolean success = ImportService.remove(folderHash);
        return RestHelper.createResponseMessage(
                success ? "Folder removed from index" : "Folder not found",
                success ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    @GetMapping("/scan")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> scan() {
        return rescan(false, false);
    }

    @GetMapping("/rescan")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> rescan(@RequestParam(value = "fully", defaultValue = "true") boolean rescanFully,
                                                  @RequestParam(value = "update_covers", defaultValue = "false") boolean forceUpdateCovers) {
        ImportService.rescan(rescanFully, forceUpdateCovers);
        return RestHelper.createResponseMessage(rescanFully ? "Rescan started" : "Scan started", HttpStatus.OK);
    }

    @GetMapping("/rescan/{hash}")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> rescan(@PathVariable(value = "hash") String folderOrSerieHash,
                                                  @RequestParam(value = "fully", defaultValue = "false") boolean rescanFully,
                                                  @RequestParam(value = "update_covers", defaultValue = "false") boolean forceUpdateCovers) {
        boolean success = BooksRepository.isSeriesHash(folderOrSerieHash)
                ? ImportService.rescanSerie(folderOrSerieHash)
                : ImportService.rescan(folderOrSerieHash, rescanFully, forceUpdateCovers);
        return RestHelper.createResponseMessage(
                success ? "Rescan started" : "Unable to start rescan",
                success ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    private <T> long countFolderItems(Class<T> clazz, String folderPath, LibraryPresentation libraryPresentation) {
        return BooksDatabaseRepository.getInstance().getDaoManager().countLike("FOLDER", folderPath, clazz, libraryPresentation);
    }

    private long countChapterItems(String folderPath) {
        BooksDaoManager daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
        return daoManager.queryLike(ImportService.FOLDER_FIELD_NAME, folderPath, BookSerie.class)
                .stream()
                .map(BaseBook.class::cast)
                .map(BaseBook::getChaptersCount)
                .mapToLong(Long::longValue)
                .sum();
    }

    public static String getStatusEndpoint() {
        return ROOT_ENDPOINT + STATUS_ENDPOINT;
    }
}
