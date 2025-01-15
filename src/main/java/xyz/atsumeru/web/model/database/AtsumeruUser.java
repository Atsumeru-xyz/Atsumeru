package xyz.atsumeru.web.model.database;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NonNull;
import xyz.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
@DatabaseTable(tableName = "USERS")
public class AtsumeruUser {
    @DatabaseField(generatedId = true)
    private Long id;

    @SerializedName("user_name")
    @DatabaseField(columnName = "USERNAME")
    private String userName;

    @Expose(serialize = false)
    @DatabaseField(columnName = "PASSWORD")
    private String password;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @DatabaseField(columnName = "ROLES")
    private String roles;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @DatabaseField(columnName = "AUTHORITIES")
    private String authorities;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("allowed_categories")
    @DatabaseField(columnName = "ALLOWED_CATEGORIES")
    private String allowedCategories;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("disallowed_genres")
    @DatabaseField(columnName = "DISALLOWED_GENRES")
    private String disallowedGenres;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("disallowed_tags")
    @DatabaseField(columnName = "DISALLOWED_TAGS")
    private String disallowedTags;

    public Set<String> getAuthoritiesSet() {
        Set<String> authorities = new HashSet<>(ArrayUtils.splitString(getAuthorities(), ","));

        List<String> roles = ArrayUtils.splitString(getRoles(), ",");
        roles.forEach(role -> authorities.add(role.startsWith("ROLE_") ? role : "ROLE_" + role));

        return authorities;
    }

    public Map<String, Category> getAllowedCategoriesMap() {
        Map<String, Category> allowedCategories = new HashMap<>();
        List<String> categoryIds = ArrayUtils.splitString(getAllowedCategories(), ",");
        categoryIds.forEach(id -> {
            Category category = CategoryRepository.getCategoryById(id);
            if (category != null) {
                allowedCategories.put(category.getCategoryId(), category);
            }
        });
        return allowedCategories;
    }

    public List<String> getAllowedContentTypes() {
        return getAllowedCategoriesMap().values().stream()
                .map(Category::getContentType)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    public List<String> getAllowedCategoryIds() {
        return getAllowedCategoriesMap().values()
                .stream()
                .map(category -> CategoryRepository.createDbIdForCategoryId(category.getCategoryId()))
                .collect(Collectors.toList());
    }

    @NonNull
    public Set<String> getDisallowedGenres() {
        return Optional.ofNullable(disallowedGenres)
                .filter(StringUtils::isNotEmpty)
                .map(genres -> ArrayUtils.splitString(genres, ","))
                .map(genres -> genres.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    @NonNull
    public Set<String> getDisallowedTags() {
        return Optional.ofNullable(disallowedTags)
                .filter(StringUtils::isNotEmpty)
                .map(tags -> ArrayUtils.splitString(tags, ","))
                .map(tags -> tags.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }
}
