package xyz.atsumeru.web.controller.rest.sync;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.database.History;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.repository.HistoryRepository;
import xyz.atsumeru.web.security.repository.UsersRepository;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.TypeUtils;

import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("/api/v1/books/sync")
public class SyncApiController {
    private final UsersRepository userService;

    public SyncApiController(UsersRepository userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/push")
    @CacheEvict(cacheNames = {"history", "books", "books_by_bound_service"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> getPushReadHistory(HttpServletRequest request,
                                                              @RequestParam(value = "hash", required = false) String hash,
                                                              @RequestParam(value = "archive_hash", required = false) String archiveHash,
                                                              @RequestParam(value = "chapter_hash", required = false) String chapterHash,
                                                              @RequestParam(value = "page") int page) {
        HistoryRepository.saveReadedPage(userService.getUserFromRequest(request), StringUtils.getFirstNotEmptyValue(hash, archiveHash), chapterHash, page);
        return RestHelper.createResponseMessage("Synced successfully", HttpStatus.OK);
    }

    @PostMapping(value = "/push")
    @CacheEvict(cacheNames = {"history", "books", "books_by_bound_service"}, allEntries = true)
    public ResponseEntity<AtsumeruMessage> postPushReadHistory(HttpServletRequest request, @RequestBody MultiValueMap<String, String> formData) {
        if (ArrayUtils.isNotEmpty(formData)) {
            for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
                String hash = entry.getKey();
                HistoryRepository.saveReadedPage(
                        userService.getUserFromRequest(request),
                        BooksRepository.isArchiveHash(hash) ? hash : null,
                        BooksRepository.isChapterHash(hash) ? hash : null,
                        TypeUtils.getIntDef(entry.getValue().get(0), 0)
                );
            }
            return RestHelper.createResponseMessage("Synced successfully", HttpStatus.OK);
        }
        return RestHelper.createResponseMessage("Sync error. No form_data values", HttpStatus.NOT_ACCEPTABLE.value(), HttpStatus.OK);
    }

    @GetMapping(value = "/pull/{book_or_archive_hash}")
    public List<History> getBookHistory(HttpServletRequest request, @PathVariable(value = "book_or_archive_hash") String bookOrArchiveHash) {
        return HistoryRepository.getBookHistory(userService.getUserFromRequest(request), bookOrArchiveHash);
    }
}
