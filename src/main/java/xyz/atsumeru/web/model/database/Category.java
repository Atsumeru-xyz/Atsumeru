package xyz.atsumeru.web.model.database;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import xyz.atsumeru.web.component.Localization;
import xyz.atsumeru.web.enums.ContentType;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.util.StringUtils;

@Getter
@DatabaseTable(tableName = "CATEGORIES")
public class Category {
    public static final String CATEGORY_TAG = "atsumeru-category";

    @Exclude
    @SerializedName("_id")
    @DatabaseField(generatedId = true)
    private Long id;

    @SerializedName("id")
    @DatabaseField(columnName = "CATEGORY_ID")
    private String categoryId;

    @Setter
    @DatabaseField(columnName = "NAME")
    private String name;

    @SerializedName("content_type")
    @DatabaseField(columnName = "CONTENT_TYPE")
    private String contentType;

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
                Localization.toLocale("enum." + contentType.name().toLowerCase()),
                contentType,
                order
        );
    }
}
