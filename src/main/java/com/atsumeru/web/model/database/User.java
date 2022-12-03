package com.atsumeru.web.model.database;

import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@Data
@DatabaseTable(tableName = "USERS")
public class User {
    @Expose
    @DatabaseField(generatedId = true)
    private Long id;

    @Expose
    @SerializedName("user_name")
    @DatabaseField(columnName = "USERNAME")
    private String userName;

    @Expose(serialize = false)
    @DatabaseField(columnName = "PASSWORD")
    private String password;

    @Expose
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @DatabaseField(columnName = "ROLES")
    private String roles;

    @Expose
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @DatabaseField(columnName = "AUTHORITIES")
    private String authorities;

    @Expose
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("allowed_categories")
    @DatabaseField(columnName = "ALLOWED_CATEGORIES")
    private String allowedCategories;

    @Expose
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("disallowed_genres")
    @DatabaseField(columnName = "DISALLOWED_GENRES")
    private String disallowedGenres;

    @Expose
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("disallowed_tags")
    @DatabaseField(columnName = "DISALLOWED_TAGS")
    private String disallowedTags;

    public Set<String> getAuthoritiesSet() {
        Set<String> authorities = new HashSet<>(GUArray.splitString(getAuthorities(), ","));

        List<String> roles = GUArray.splitString(getRoles(), ",");
        roles.forEach(role -> authorities.add(role.startsWith("ROLE_") ? role : "ROLE_" + role));

        return authorities;
    }

    public Map<String, Category> getAllowedCategoriesMap() {
        Map<String, Category> allowedCategories = new HashMap<>();
        List<String> categoryIds = GUArray.splitString(getAllowedCategories(), ",");
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
                .filter(GUString::isNotEmpty)
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
                .filter(GUString::isNotEmpty)
                .map(genres -> GUArray.splitString(genres, ","))
                .map(genres -> genres.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    @NonNull
    public Set<String> getDisallowedTags() {
        return Optional.ofNullable(disallowedTags)
                .filter(GUString::isNotEmpty)
                .map(tags -> GUArray.splitString(tags, ","))
                .map(tags -> tags.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }
}
