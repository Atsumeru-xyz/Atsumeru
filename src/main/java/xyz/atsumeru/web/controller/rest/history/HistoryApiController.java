package xyz.atsumeru.web.controller.rest.history;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.repository.HistoryRepository;
import xyz.atsumeru.web.security.repository.UsersRepository;

import java.util.List;

@Controller
@RestController
@RequestMapping("/api/v1/books")
public class HistoryApiController {
    private final UsersRepository userService;

    public HistoryApiController(UsersRepository userService) {
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
