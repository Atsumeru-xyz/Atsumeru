package xyz.atsumeru.web.repository;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.ContentType;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.database.Category;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.EnumUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@DependsOn("booksDaoManager")
public class CategoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(CategoryRepository.class.getSimpleName());

    @Getter
    private static final List<Category> categories = new ArrayList<>();

    public CategoryRepository() {
        init();
    }

    public void init() {
        long time = System.currentTimeMillis();
        loadCategories();
        logger.info("Categories loaded. Took " + (System.currentTimeMillis() - time) + "ms");
    }

    public void setCategories(MultiValueMap<String, String> contentIdsWithCategories) {
        Beans.getBooksDaoManager().setAutoCommit(false);

        List<IBaseBookItem> items = Beans.getBooksDaoManager().queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES);
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

                    String categories = StringUtils.join(",", dbCategories);
                    bookItem.setCategories(StringUtils.isNotEmpty(categories) ? categories : null);
                })
                .collect(Collectors.toList());

        items.forEach(Beans.getBooksDaoManager()::save);

        Beans.getBooksDaoManager().commit();
        Beans.getBooksDaoManager().setAutoCommit(true);
    }

    public void orderCategories(List<Category> changedCategories) {
        changedCategories.forEach(changedCategory -> {
            Category categoryInDb = CategoryRepository.categories
                    .stream()
                    .filter(category -> StringUtils.equalsIgnoreCase(category.getCategoryId(), changedCategory.getCategoryId()))
                    .findFirst()
                    .orElse(null);

            if (categoryInDb != null) {
                categoryInDb.setOrder(changedCategory.getOrder());
                Beans.getBooksDaoManager().save(categoryInDb);
            }
        });

        sortCategories();
    }

    public boolean createCategory(String categoryName) {
        boolean hasCategory = getCategories().stream()
                .anyMatch(category -> StringUtils.equalsIgnoreCase(categoryName, category.getName()));

        if (!hasCategory) {
            Beans.getBooksDaoManager().save(Category.createFromName(categoryName, CategoryRepository.getLastCategoryOrder()));
            reLoadCategories();
            return true;
        }

        return false;
    }

    public boolean editCategory(String categoryId, String categoryName) {
        Category category = getCategoryById(categoryId);

        if (category != null) {
            category.setName(categoryName);
            Beans.getBooksDaoManager().save(category);
            reLoadCategories();
            return true;
        }

        return false;
    }

    public boolean deleteCategory(String categoryId) {
        boolean hasCategory = getCategories().stream()
                .anyMatch(category -> StringUtils.equalsIgnoreCase(categoryId, category.getCategoryId()));

        if (hasCategory) {
            Beans.getBooksDaoManager().removeByColumnIn("CATEGORY_ID", Collections.singletonList(categoryId), Category.class);
            reLoadCategories();
            return true;
        }

        return false;
    }

    public static ContentType getContentTypeForCategory(String categoryId, ContentType defaultType) {
        Category category = getCategoryById(categoryId);
        return Optional.ofNullable(category)
                .map(value -> EnumUtils.valueOfOrNull(ContentType.class, category.getContentType()))
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
        if (category != null && StringUtils.isEmpty(category.getContentType())) {
            return createDbIdForCategoryRealId(String.valueOf(category.getId()));
        }
        return null;
    }

    public static String createDbIdForCategory(Category category) {
        return createDbIdForCategoryRealId(String.valueOf(category.getId()));
    }

    public static int getLastCategoryOrder() {
        Category lastCategory = ArrayUtils.getLastItem(CategoryRepository.getCategories());
        if (lastCategory != null) {
            return lastCategory.getOrder() + 1;
        }
        return 0;
    }

    public void reLoadCategories() {
        loadCategories();
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(Beans.getBooksDaoManager().queryAll(Category.class));

        ContentType.getSupportedTypes().forEach(contentType -> {
            long count = Beans.getBooksDaoManager().countForContentType(contentType, BookSerie.class);
            if (count > 0 && categories.stream().noneMatch(category -> StringUtils.equalsIgnoreCase(contentType.name(), category.getContentType()))) {
                Category category = Category.createFromContentType(contentType, CategoryRepository.getLastCategoryOrder());
                categories.add(category);
                Beans.getBooksDaoManager().save(category);
            }
        });

        sortCategories();
    }

    private static void sortCategories() {
        categories.sort(Comparator.comparingInt(Category::getOrder));
    }

    public static Category getCategoryById(String id) {
        return categories.stream()
                .filter(category -> StringUtils.equalsIgnoreCase(category.getCategoryId(), id))
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
        return ArrayUtils.isEmpty(allowedCategories) || allowedCategories.containsKey(category.getCategoryId());
    }
}
