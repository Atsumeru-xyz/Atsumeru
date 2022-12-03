package com.atsumeru.web.controller.rest.hub;

import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.UserDatabaseRepository;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.enums.Sort;
import com.atsumeru.web.helper.ServerHelper;
import com.atsumeru.web.manager.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RestController
@RequestMapping("/api/v1/books")
public class HubApiController {
    @Autowired
    UserDatabaseRepository userService;

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
