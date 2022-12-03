package com.atsumeru.web.model.database;

import com.atsumeru.web.model.book.BookSerie;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

@Data
@DatabaseTable(tableName = "HISTORY")
public class History {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "USER_ID")
    private Long userId;

    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;

    @Expose
    @SerializedName("serie_hash")
    @DatabaseField(columnName = "SERIE_HASH")
    private String serieHash;

    @Expose
    @SerializedName("archive_hash")
    @DatabaseField(columnName = "ARCHIVE_HASH")
    private String archiveHash;

    @Expose
    @SerializedName("chapter_hash")
    @DatabaseField(columnName = "CHAPTER_HASH")
    private String chapterHash;

    @Expose
    @SerializedName("current_page")
    @DatabaseField(columnName = "CURRENT_PAGE")
    private Integer currentPage;

    @Expose
    @SerializedName("pages_count")
    @DatabaseField(columnName = "PAGES_COUNT")
    private Integer pagesCount;

    @Expose
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
