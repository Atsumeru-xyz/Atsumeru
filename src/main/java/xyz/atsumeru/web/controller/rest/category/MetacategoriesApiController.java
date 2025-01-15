package xyz.atsumeru.web.controller.rest.category;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.category.Metacategory;
import xyz.atsumeru.web.repository.MetacategoryRepository;
import xyz.atsumeru.web.security.repository.UsersRepository;

import java.util.List;
import java.util.Set;

@Controller
@RestController
@RequestMapping("/api/v1/books/metacategories")
public class MetacategoriesApiController {
    private final UsersRepository userService;

    public MetacategoriesApiController(UsersRepository userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public Set<Metacategory> getMetacategoryList() {
        return MetacategoryRepository.getMetacategories();
    }

    @GetMapping("/{metacategory_id}")
    public List<Metacategory> getMetacategoryEntries(@PathVariable(value = "metacategory_id") String metacategoryId) {
        return MetacategoryRepository.getEntries(metacategoryId);
    }

    @GetMapping("/{metacategory_id}/{filter}")
    public List<IBaseBookItem> getMetacategoryEntries(HttpServletRequest request,
                                                      @PathVariable(value = "metacategory_id") String metacategoryId,
                                                      @PathVariable(value = "filter") String filter,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "limit", defaultValue = "30") int limit,
                                                      @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                                      @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        return MetacategoryRepository.getFilteredList(userService.getUserFromRequest(request), metacategoryId, filter, page, limit, withVolumesAndHistory, withChapters);
    }
}
