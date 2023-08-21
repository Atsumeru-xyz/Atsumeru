package com.atsumeru.web.model.database;

import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.enums.ContentType;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@DatabaseTable(tableName = "CATEGORIES")
public class Category {
    public static final String CATEGORY_TAG = "atsumeru-category";

    @DatabaseField(generatedId = true)
    private Long id;

    @Expose
    @SerializedName("id")
    @DatabaseField(columnName = "CATEGORY_ID")
    private String categoryId;

    @Expose
    @Setter
    @DatabaseField(columnName = "NAME")
    private String name;

    @Expose
    @SerializedName("content_type")
    @DatabaseField(columnName = "CONTENT_TYPE")
    private String contentType;

    @Expose
    @Setter
    @DatabaseField(columnName = "SORT_ORDER")
    private Integer order;

    public Category() {
    }

    public Category(String categoryId, String name, @Nullable ContentType contentType, int order) {
        this.categoryId = categoryId;
        this.name = name;
        if (contentType != null) {
            this.contentType = contentType.name();
        }
        this.order = order;
    }

    public static Category createFromName(String categoryName, int order) {
        return new Category(CATEGORY_TAG + StringUtils.md5Hex(categoryName), categoryName, null, order);
    }

    public static Category createFromContentType(ContentType contentType, int order) {
        return new Category(
                CATEGORY_TAG + StringUtils.md5Hex(contentType.name() + contentType.ordinal()),
                Localizr.toLocale("enum." + contentType.name().toLowerCase()),
                contentType,
                order
        );
    }
}
