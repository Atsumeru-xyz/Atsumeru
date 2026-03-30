package xyz.atsumeru.web.model.database;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.model.book.BookSerie;

@Data
@DatabaseTable(tableName = "HISTORY")
public class History {
    @Exclude
    @Schema(name = "_id")
    @SerializedName("_id")
    @DatabaseField(generatedId = true)
    private Long id;

    @Exclude
    @DatabaseField(columnName = "USER_ID")
    private Long userId;

    @Exclude
    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;

    @Schema(name = "serie_hash")
    @SerializedName("serie_hash")
    @DatabaseField(columnName = "SERIE_HASH")
    private String serieHash;

    @Schema(name = "archive_hash")
    @SerializedName("archive_hash")
    @DatabaseField(columnName = "ARCHIVE_HASH")
    private String archiveHash;

    @Schema(name = "chapter_hash")
    @SerializedName("chapter_hash")
    @DatabaseField(columnName = "CHAPTER_HASH")
    private String chapterHash;

    @Schema(name = "current_page")
    @SerializedName("current_page")
    @DatabaseField(columnName = "CURRENT_PAGE")
    private Integer currentPage;

    @Schema(name = "pages_count")
    @SerializedName("pages_count")
    @DatabaseField(columnName = "PAGES_COUNT")
    private Integer pagesCount;

    @Schema(name = "last_read_at")
    @SerializedName("last_read_at")
    @DatabaseField(columnName = "LAST_READ_AT")
    private Long lastReadAt;

    public History() {
    }

    public History(long userId, BookSerie serie, String archiveHash, String chapterHash, int pagesCount) {
        this.serie = serie;
        this.userId = userId;
        this.serieHash = serie.getSerieId();
        this.archiveHash = archiveHash;
        this.chapterHash = chapterHash;
        this.pagesCount = pagesCount;
    }

    public Long getDbId() {
        return id;
    }

    public String getBookHash(boolean isSerie) {
        return isSerie ? getSerieHash() : getArchiveHash();
    }
}
