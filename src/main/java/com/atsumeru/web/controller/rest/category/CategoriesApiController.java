package com.atsumeru.web.controller.rest.category;

import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.UserDatabaseRepository;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.util.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RestController
@RequestMapping("/api/v1/books/categories")
public class CategoriesApiController {
    @Autowired
    UserDatabaseRepository userService;

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

                    long itemsInCategory = BooksDatabaseRepository.getInstance()
                            .getDaoManager()
                            .countForCategory(
                                    BookSerie.class,
                                    CategoryRepository.createDbIdForCategoryId(category.getCategoryId()),
                                    contentType
                            );

                    return itemsInCategory > 0;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/set")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> setCategories(@RequestBody MultiValueMap<String, String> contentIdsWithCategories) {
        CategoryRepository.setCategories(contentIdsWithCategories);
        AtsumeruCacheManager.evictAll();
        return RestHelper.createResponseMessage("Categories set for provided content list", HttpStatus.OK);
    }

    @PostMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> orderCategories(@RequestBody List<Category> changedCategories) {
        CategoryRepository.orderCategories(changedCategories);
        return RestHelper.createResponseMessage("Categories ordered", HttpStatus.OK);
    }

    @PutMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> createCategory(@RequestParam(value = "name") String categoryName) {
        boolean created = CategoryRepository.createCategory(categoryName);
        return RestHelper.createResponseMessage(
                created ? "Category created" : "Category already exists",
                created ? HttpStatus.CREATED : HttpStatus.CONFLICT
        );
    }

    @PatchMapping("/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> editCategory(@RequestParam(value = "id") String categoryId,
                                                        @RequestParam(value = "name") String categoryName) {
        boolean edited = CategoryRepository.editCategory(categoryId, categoryName);
        return RestHelper.createResponseMessage(
                edited ? "Category edited" : "Unable to edit category",
                edited ? HttpStatus.OK : HttpStatus.CONFLICT
        );
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtsumeruMessage> deleteCategory(@RequestParam(value = "id") String categoryId) {
        boolean deleted = CategoryRepository.deleteCategory(categoryId);
        AtsumeruCacheManager.evictAll();
        return RestHelper.createResponseMessage(
                deleted ? "Category deleted" : "Category not exist",
                deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }
}
