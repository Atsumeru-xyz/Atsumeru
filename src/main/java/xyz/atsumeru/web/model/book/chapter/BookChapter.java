package xyz.atsumeru.web.model.book.chapter;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.json.adapter.AdminFieldAdapter;
import xyz.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.database.History;
import xyz.atsumeru.web.repository.dao.BooksDaoManager;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Data
@DatabaseTable(tableName = "BOOK_CHAPTERS")
public class BookChapter {
    @Exclude
    @SerializedName("_id")
    @DatabaseField(generatedId = true)
    private Long id;

    @Exclude
    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;

    @Exclude
    @DatabaseField(foreign = true, columnName = "ARCHIVE")
    private BookArchive archive;

    @SerializedName("id")
    @DatabaseField(columnName = "CHAPTER_ID")
    private String chapterId;

    @Exclude
    @DatabaseField(columnName = "SERIE_ID")
    private String serieId;

    @Exclude
    @DatabaseField(columnName = "ARCHIVE_ID")
    private String archiveId;

    @DatabaseField(columnName = "TITLE")
    private String title;

    @SerializedName("alt_title")
    @DatabaseField(columnName = "ALTERNATIVE_TITLE")
    private String altTitle;

    @JsonAdapter(AdminFieldAdapter.class)
    @DatabaseField(columnName = "FOLDER")
    private String folder;

    @SerializedName("chapter")
    @DatabaseField(columnName = "CHAPTER")
    private Float chapter;

    @DatabaseField(columnName = "AUTHORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String authors;

    @DatabaseField(columnName = "ARTISTS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String artists;

    @DatabaseField(columnName = "TRANSLATORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String translators;

    @DatabaseField(columnName = "LANGUAGE")
    @SerializedName("language")
    private String language;

    @DatabaseField(columnName = "PARODIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String parodies;

    @DatabaseField(columnName = "CHARACTERS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String characters;

    @DatabaseField(columnName = "CENSORSHIP")
    private String censorship;

    @DatabaseField(columnName = "COLOR")
    private String color;

    @DatabaseField(columnName = "DESCRIPTION")
    private String description;

    @DatabaseField(columnName = "GENRES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String genres;

    @DatabaseField(columnName = "TAGS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String tags;

    @SerializedName("pages_count")
    @DatabaseField(columnName = "PAGES_COUNT")
    private int pagesCount;

    @SerializedName("created_at")
    @DatabaseField(columnName = "CREATED_AT")
    private Long createdAt;

    @SerializedName("updated_at")
    @DatabaseField(columnName = "UPDATED_AT")
    private Long updatedAt;

    @Exclude
    @DatabaseField(columnName = "PAGE_ENTRY_NAMES")
    private String pageEntryNames;

    @Exclude
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
                .map(bookArchive -> AtsumeruApplication.getContext().getBean(BooksDaoManager.class).queryById(bookArchive.getDbId(), BookArchive.class))
                .orElse(null);
    }

    public Float getChapter() {
        return Optional.ofNullable(chapter).orElse(-1f);
    }
}
