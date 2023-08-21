package com.atsumeru.web.model.book.chapter;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.json.adapter.AdminFieldAdapter;
import com.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import kotlin.jvm.Transient;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@DatabaseTable(tableName = "BOOK_CHAPTERS")
public class BookChapter {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;

    @DatabaseField(foreign = true, columnName = "ARCHIVE")
    private BookArchive archive;

    @Expose
    @SerializedName("id")
    @DatabaseField(columnName = "CHAPTER_ID")
    private String chapterId;

    @DatabaseField(columnName = "SERIE_ID")
    private String serieId;

    @DatabaseField(columnName = "ARCHIVE_ID")
    private String archiveId;

    @Expose
    @DatabaseField(columnName = "TITLE")
    private String title;

    @Expose
    @SerializedName("alt_title")
    @DatabaseField(columnName = "ALTERNATIVE_TITLE")
    private String altTitle;

    @Expose
    @JsonAdapter(AdminFieldAdapter.class)
    @DatabaseField(columnName = "FOLDER")
    private String folder;

    @Expose
    @SerializedName("chapter")
    @DatabaseField(columnName = "CHAPTER")
    private Float chapter;

    @Expose
    @DatabaseField(columnName = "AUTHORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String authors;

    @Expose
    @DatabaseField(columnName = "ARTISTS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String artists;

    @Expose
    @DatabaseField(columnName = "TRANSLATORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String translators;

    @Expose
    @DatabaseField(columnName = "LANGUAGE")
    @SerializedName("language")
    private String language;

    @Expose
    @DatabaseField(columnName = "PARODIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String parodies;

    @Expose
    @DatabaseField(columnName = "CHARACTERS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String characters;

    @Expose
    @DatabaseField(columnName = "CENSORSHIP")
    private String censorship;

    @Expose
    @DatabaseField(columnName = "COLOR")
    private String color;

    @DatabaseField(columnName = "DESCRIPTION")
    private String description;

    @Expose
    @DatabaseField(columnName = "GENRES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String genres;

    @Expose
    @DatabaseField(columnName = "TAGS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String tags;

    @Expose
    @SerializedName("pages_count")
    @DatabaseField(columnName = "PAGES_COUNT")
    private int pagesCount;

    @Expose
    @SerializedName("created_at")
    @DatabaseField(columnName = "CREATED_AT")
    private Long createdAt;

    @Expose
    @SerializedName("updated_at")
    @DatabaseField(columnName = "UPDATED_AT")
    private Long updatedAt;

    @DatabaseField(columnName = "PAGE_ENTRY_NAMES")
    private String pageEntryNames;

    @Expose
    @Transient
    private History history;

    public BookChapter() {
    }

    public BookChapter(String title, String chapterFolder, String archiveHash) {
        this.title = title;
        generateChapterId(archiveHash);
        folder = chapterFolder;
    }

    public void generateChapterId(String archiveHash) {
        chapterId = StringUtils.md5Hex(archiveHash + title);
    }

    public void setSerie(BookSerie serie) {
        this.serie = serie;
        serieId = serie.getContentId();
    }

    public void setArchive(BookArchive archive) {
        this.archive = archive;
        archiveId = archive.getContentId();
    }

    public void setPageEntryNames(List<String> pageEntryNames) {
        this.pageEntryNames = StringUtils.join(".|.", pageEntryNames);
        pagesCount = pageEntryNames.size();
    }

    public List<String> getPageEntryNames() {
        return ArrayUtils.splitString(pageEntryNames, ".\\|.");
    }

    public BookArchive getArchive() {
        return (BookArchive) Optional.ofNullable(archive)
                .map(bookArchive -> BooksDatabaseRepository.getInstance().getDaoManager().queryById(bookArchive.getDbId(), BookArchive.class))
                .orElse(null);
    }

    public Float getChapter() {
        return Optional.ofNullable(chapter).orElse(-1f);
    }
}
