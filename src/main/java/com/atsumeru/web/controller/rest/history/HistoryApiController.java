package com.atsumeru.web.controller.rest.history;

import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.HistoryRepository;
import com.atsumeru.web.repository.UserDatabaseRepository;
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
public class HistoryApiController {
    private final UserDatabaseRepository userService;

    public HistoryApiController(UserDatabaseRepository userService) {
        this.userService = userService;
    }

    //*****************************//
    //*          History          *//
    //*****************************//
    @GetMapping("/history")
    @Cacheable(value = "history", key="#request.userPrincipal.name.concat('-')" +
            ".concat(#libraryPresentation.toString()).concat('-')" +
            ".concat(#page).concat('-')" +
            ".concat(#limit).concat('-')")
    public List<IBaseBookItem> getBooksHistory(HttpServletRequest request,
                                               @RequestParam(value = "presentation", defaultValue = "series") LibraryPresentation libraryPresentation,
                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "limit", defaultValue = "50") long limit) {
        return HistoryRepository.getBooksHistory(userService.getUserFromRequest(request), libraryPresentation, page, limit);
    }
}
