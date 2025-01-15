package xyz.atsumeru.web.controller.rest.hub;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.atsumeru.web.enums.ContentType;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.enums.Sort;
import xyz.atsumeru.web.helper.ServerHelper;
import xyz.atsumeru.web.manager.Settings;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.security.repository.UsersRepository;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/v1/books")
public class HubApiController {
    private final UsersRepository userService;

    public HubApiController(UsersRepository userService) {
        this.userService = userService;
    }

    //*****************************//
    //*    Hub: New and Latest    *//
    //*****************************//
    @GetMapping(value = {
            "/new",
            "/updates"
    })
    @Cacheable(value = "hub-updates", key="#request.userPrincipal.name.concat('-')" +
            ".concat(#request.servletPath).concat('-')" +
            ".concat(#libraryPresentation).concat('-')" +
            ".concat(\"\" + #contentType).concat('-')" +
            ".concat(#category).concat('-')" +
            ".concat(#ascendingOrder).concat('-')" +
            ".concat(#page).concat('-')" +
            ".concat(#limit).concat('-')" +
            ".concat(#withVolumesAndHistory).concat('-')" +
            ".concat(#withChapters).concat('-')")
    public List<IBaseBookItem> getBooksHubInfo(HttpServletRequest request,
                                               @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation,
                                               @RequestParam(value = "type", defaultValue = "") ContentType contentType,
                                               @RequestParam(value = "category", defaultValue = "") String category,
                                               @RequestParam(value = "asc", defaultValue = "false") boolean ascendingOrder,
                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "limit", defaultValue = "50") long limit,
                                               @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                               @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return BooksRepository.getBooks(
                userService.getUserFromRequest(request),
                libraryPresentation,
                CategoryRepository.getContentTypeForCategory(category, contentType),
                CategoryRepository.createDbIdForCategoryId(category),
                getOrderByStringForHubRequest(request),
                ascendingOrder,
                page,
                limit,
                Settings.isAllowListLoadingWithVolumes() && withVolumesAndHistory,
                Settings.isAllowListLoadingWithChapters() && withChapters,
                false);
    }

    private Sort getOrderByStringForHubRequest(HttpServletRequest request) {
        String path = ServerHelper.getRequestedURLPath(request);
        if (path.contains("/new/") || path.endsWith("/new")) {
            return Sort.CREATED_AT;
        } else if (path.contains("/updates/") || path.endsWith("/updates")) {
            return Sort.UPDATED_AT;
        }
        return null;
    }
}
