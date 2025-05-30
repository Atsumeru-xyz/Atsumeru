package xyz.atsumeru.web.controller.rest.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.ContentType;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.manager.cache.AtsumeruCache;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.database.Category;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.security.repository.UsersRepository;
import xyz.atsumeru.web.util.EnumUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RestController
@RequestMapping("/api/v1/books/categories")
@Tag(name = "Categories", description = "API for requesting Categories specific info")
public class CategoriesApiController {
    private final UsersRepository userService;

    public CategoriesApiController(UsersRepository userService) {
        this.userService = userService;
    }

    @Operation(summary = "Categories list", description = "Get list of available created Categories")
    @GetMapping("")
    public List<Category> getCategoryList(HttpServletRequest request) {
        Map<String, Category> allowedCategories = userService.getUserFromRequest(request).getAllowedCategoriesMap();
        return CategoryRepository.getCategories()
                .stream()
                .filter(category -> CategoryRepository.isCategoryAllowedForUser(category, allowedCategories))
                .filter(category -> {
                    ContentType contentType = EnumUtils.valueOfOrNull(ContentType.class, category.getContentType());
                    if (contentType == null) {
                        return true;
                    }

                    return Beans.getBooksDaoManager().countForCategory(
                            BookSerie.class,
                            CategoryRepository.createDbIdForCategoryId(category.getCategoryId()),
                            contentType
                    ) > 0;
                })
                .collect(Collectors.toList());
    }

    @Operation(summary = "Set Categories", description = "Associate Books by Hash with Categories by Category id")
    @PostMapping("/set")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> setCategories(@RequestBody MultiValueMap<String, String> contentIdsWithCategories) {
        AtsumeruApplication.getContext().getBean(CategoryRepository.class).setCategories(contentIdsWithCategories);
        AtsumeruCache.evictAll();
        return RestHelper.createResponseMessage("Categories set for provided content list", HttpStatus.OK);
    }

    @Operation(summary = "Order Categories", description = "Change Categories order")
    @PostMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> orderCategories(@RequestBody List<Category> changedCategories) {
        AtsumeruApplication.getContext().getBean(CategoryRepository.class).orderCategories(changedCategories);
        return RestHelper.createResponseMessage("Categories ordered", HttpStatus.OK);
    }

    @Operation(summary = "Create Category", description = "Create new Category with provided unique name")
    @PutMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> createCategory(@RequestParam(value = "name") String categoryName) {
        boolean created = AtsumeruApplication.getContext().getBean(CategoryRepository.class).createCategory(categoryName);
        return RestHelper.createResponseMessage(
                created ? "Category created" : "Category already exists",
                created ? HttpStatus.CREATED : HttpStatus.CONFLICT
        );
    }

    @Operation(summary = "Edit Category", description = "Edit Category name by id")
    @PatchMapping("/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> editCategory(@RequestParam(value = "id") String categoryId,
                                                        @RequestParam(value = "name") String categoryName) {
        boolean edited = AtsumeruApplication.getContext().getBean(CategoryRepository.class).editCategory(categoryId, categoryName);
        return RestHelper.createResponseMessage(
                edited ? "Category edited" : "Unable to edit category",
                edited ? HttpStatus.OK : HttpStatus.CONFLICT
        );
    }

    @Operation(summary = "Delete Category", description = "Delete Category by id")
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> deleteCategory(@RequestParam(value = "id") String categoryId) {
        boolean deleted = AtsumeruApplication.getContext().getBean(CategoryRepository.class).deleteCategory(categoryId);
        AtsumeruCache.evictAll();
        return RestHelper.createResponseMessage(
                deleted ? "Category deleted" : "Category not exist",
                deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }
}
