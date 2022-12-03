package com.atsumeru.web.repository;

import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUEnum;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.model.database.Category;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CategoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(CategoryRepository.class.getSimpleName());
    private static final BooksDaoManager daoManager;

    @Getter
    private static final List<Category> categories = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void init() {
        long time = System.currentTimeMillis();
        loadCategories();
        logger.info("Categories loaded. Took " + (System.currentTimeMillis() - time) + "ms");
    }

    public static void setCategories(MultiValueMap<String, String> contentIdsWithCategories) {
        daoManager.setAutoCommit(false);

        List<IBaseBookItem> items = daoManager.queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES);
        items = items.stream()
                .filter(bookItem -> contentIdsWithCategories.containsKey(bookItem.getContentId()))
                .peek(bookItem -> {
                    List<String> dbCategories = contentIdsWithCategories.get(bookItem.getContentId())
                            .stream()
                            .flatMap(Pattern.compile(",")::splitAsStream)
                            .map(CategoryRepository::getCategoryById)
                            .filter(Objects::nonNull)
                            .map(category -> String.valueOf(category.getId()))
                            .map(CategoryRepository::createDbIdForCategoryRealId)
                            .collect(Collectors.toList());

                    String categories = GUString.join(",", dbCategories);
                    bookItem.setCategories(GUString.isNotEmpty(categories) ? categories : null);
                })
                .collect(Collectors.toList());

        items.forEach(daoManager::save);

        daoManager.commit();
        daoManager.setAutoCommit(true);
    }

    public static void orderCategories(List<Category> changedCategories) {
        changedCategories.forEach(changedCategory -> {
            Category categoryInDb = CategoryRepository.categories
                    .stream()
                    .filter(category -> GUString.equalsIgnoreCase(category.getCategoryId(), changedCategory.getCategoryId()))
                    .findFirst()
                    .orElse(null);

            if (categoryInDb != null) {
                categoryInDb.setOrder(changedCategory.getOrder());
                daoManager.save(categoryInDb);
            }
        });

        sortCategories();
    }

    public static boolean createCategory(String categoryName) {
        boolean hasCategory = getCategories().stream()
                .anyMatch(category -> GUString.equalsIgnoreCase(categoryName, category.getName()));

        if (!hasCategory) {
            daoManager.save(Category.createFromName(categoryName, CategoryRepository.getLastCategoryOrder()));
            reLoadCategories();
            return true;
        }

        return false;
    }

    public static boolean editCategory(String categoryId, String categoryName) {
        Category category = getCategoryById(categoryId);

        if (category != null) {
            category.setName(categoryName);
            daoManager.save(category);
            reLoadCategories();
            return true;
        }

        return false;
    }

    public static boolean deleteCategory(String categoryId) {
        boolean hasCategory = getCategories().stream()
                .anyMatch(category -> GUString.equalsIgnoreCase(categoryId, category.getCategoryId()));

        if (hasCategory) {
            daoManager.removeByColumnIn("CATEGORY_ID", Collections.singletonList(categoryId), Category.class);
            reLoadCategories();
            return true;
        }

        return false;
    }

    public static ContentType getContentTypeForCategory(String categoryId, ContentType defaultType) {
        Category category = getCategoryById(categoryId);
        return Optional.ofNullable(category)
                .map(value -> GUEnum.valueOfOrNull(ContentType.class, category.getContentType()))
                .orElse(defaultType);
    }

    public static String getRealIdFromCategoryDbId(String id) {
        return id.replace("{", "").replace("}", "");
    }

    public static String createDbIdForCategoryRealId(String id) {
        return !id.startsWith("{") ? String.format("{%s}", id) : id;
    }

    public static String createDbIdForCategoryId(String categoryId) {
        Category category = getCategoryById(categoryId);
        if (category != null && GUString.isEmpty(category.getContentType())) {
            return createDbIdForCategoryRealId(String.valueOf(category.getId()));
        }
        return null;
    }

    public static String createDbIdForCategory(Category category) {
        return createDbIdForCategoryRealId(String.valueOf(category.getId()));
    }

    public static int getLastCategoryOrder() {
        Category lastCategory = GUArray.getLastItem(CategoryRepository.getCategories());
        if (lastCategory != null) {
            return lastCategory.getOrder() + 1;
        }
        return 0;
    }

    public static void reLoadCategories() {
        loadCategories();
    }

    private static void loadCategories() {
        categories.clear();
        categories.addAll(daoManager.queryAll(Category.class));

        ContentType.getSupportedTypes().forEach(contentType -> {
            long count = daoManager.countForContentType(contentType, BookSerie.class);
            if (count > 0 && categories.stream().noneMatch(category -> GUString.equalsIgnoreCase(contentType.name(), category.getContentType()))) {
                Category category = Category.createFromContentType(contentType, CategoryRepository.getLastCategoryOrder());
                categories.add(category);
                daoManager.save(category);
            }
        });

        sortCategories();
    }

    private static void sortCategories() {
        categories.sort(Comparator.comparingInt(Category::getOrder));
    }

    public static Category getCategoryById(String id) {
        return categories.stream()
                .filter(category -> GUString.equalsIgnoreCase(category.getCategoryId(), id))
                .findFirst()
                .orElse(null);
    }

    public static Category getCategoryByDbId(Long id) {
        return categories.stream()
                .filter(category -> category.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static boolean isCategoryAllowedForUser(Category category, Map<String, Category> allowedCategories) {
        return GUArray.isEmpty(allowedCategories) || allowedCategories.containsKey(category.getCategoryId());
    }

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
    }
}
