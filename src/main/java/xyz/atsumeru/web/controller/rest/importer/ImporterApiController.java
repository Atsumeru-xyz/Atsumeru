package xyz.atsumeru.web.controller.rest.importer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.importer.Importer;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.book.BaseBook;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.importer.ImportFolder;
import xyz.atsumeru.web.model.importer.ImportStatus;
import xyz.atsumeru.web.properties.ImportFolders;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.service.ImportService;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping(ImporterApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('IMPORTER')")
@Tag(name = "Importer", description = "API for controlling Importer service")
public class ImporterApiController {
    protected static final String ROOT_ENDPOINT = "/api/v1/importer";
    private static final String STATUS_ENDPOINT = "/status";

    @Operation(summary = "Status", description = "Get import status info")
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

    @Operation(summary = "List import folders", description = "Get list of Importer import folders")
    @GetMapping("/list")
    public List<ImportFolder> listFolders() {
        List<ImportFolder> properties = ImportFolders.getFolderProperties();
        properties.forEach(property -> {
            property.setSeriesCount(countFolderItems(BookSerie.class, property.getPath(), LibraryPresentation.SERIES));
            property.setSinglesCount(countFolderItems(BookSerie.class, property.getPath(), LibraryPresentation.SINGLES));
            property.setArchivesCount(countFolderItems(BookArchive.class, property.getPath(), LibraryPresentation.ARCHIVES));
            property.setChaptersCount(countChapterItems(property.getPath()));
        });

        return properties;
    }

    @Operation(summary = "Add import folders", description = "Add new import folder into Importer")
    @PostMapping("/add")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> addFolder(@RequestBody ImportFolder importFolder) {
        File path = new File(importFolder.getPath());
        boolean isDirectoryExistAndNotAdded = path.exists() && path.isDirectory()
                && !ImportFolders.containsFolder(importFolder.getPath());

        if (isDirectoryExistAndNotAdded) {
            importFolder.createHash();
            ImportFolders.addFolder(importFolder);
            ImportService.add(importFolder);
        }

        return RestHelper.createResponseMessage(
                isDirectoryExistAndNotAdded ? "Folder added" : "Folder not added",
                isDirectoryExistAndNotAdded ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    @Operation(summary = "Remove import folder", description = "Remove import folder and associated Books from Importer")
    @DeleteMapping("/remove/{hash}")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> removeFolder(@PathVariable(value = "hash") String folderHash) {
        boolean success = ImportService.remove(folderHash);
        return RestHelper.createResponseMessage(
                success ? "Folder removed from index" : "Folder not found",
                success ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

    @Operation(summary = "Request scan", description = "Request Importer scan of Books in import folders. This operation only search for new or deleted files")
    @GetMapping("/scan")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> scan() {
        return rescan(false, false);
    }

    @Operation(summary = "Request rescan", description = "Request Importer rescan of Books in import folders")
    @GetMapping("/rescan")
    @CacheEvict(cacheNames = {"books", "books_by_bound_service", "filters", "hub-updates", "history"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> rescan(@RequestParam(value = "fully", defaultValue = "true") boolean rescanFully,
                                                  @RequestParam(value = "update_covers", defaultValue = "false") boolean forceUpdateCovers) {
        ImportService.rescan(rescanFully, forceUpdateCovers);
        return RestHelper.createResponseMessage(rescanFully ? "Rescan started" : "Scan started", HttpStatus.OK);
    }

    @Operation(summary = "Request folder or Serie rescan", description = "Request Importer specific rescan of Folder of Book by hash")
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
        return Beans.getBooksDaoManager().countLike("FOLDER", folderPath, clazz, libraryPresentation);
    }

    private long countChapterItems(String folderPath) {
        return Beans.getBooksDaoManager()
                .queryLike(ImportService.FOLDER_FIELD_NAME, folderPath, BookSerie.class)
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
