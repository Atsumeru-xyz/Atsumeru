package xyz.atsumeru.web.model.book;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.component.Localization;
import xyz.atsumeru.web.enums.ContentType;
import xyz.atsumeru.web.enums.Status;
import xyz.atsumeru.web.json.adapter.LinksBidirectionalAdapter;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.book.volume.VolumeItem;
import xyz.atsumeru.web.model.database.History;
import xyz.atsumeru.web.repository.dao.BooksDaoManager;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.TypeUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@DatabaseTable(tableName = "BOOK_ARCHIVES")
public class BookArchive extends BaseBook {
    @Exclude
    transient String serieHash;

    @Exclude
    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;

    @Exclude
    @ForeignCollectionField(columnName = "CHAPTERS")
    private ForeignCollection<BookChapter> chapters;

    @Schema(name = "id")
    @SerializedName("id")
    @DatabaseField(columnName = "MANGA_ID")
    private String mangaId;

    @Schema(name = "link")
    @SerializedName("link")
    @DatabaseField(columnName = "MANGA_LINK")
    private String mangaLink;

    @Schema(name = "links")
    @SerializedName("links")
    @DatabaseField(columnName = "MANGA_LINKS")
    @JsonAdapter(LinksBidirectionalAdapter.class)
    private String mangaLinks;

    @DatabaseField(columnName = "TITLE")
    private String title;

    @Schema(name = "volume")
    @SerializedName("volume")
    @DatabaseField(columnName = "VOLUME")
    private Float volume;

    @Exclude
    @DatabaseField(columnName = "PAGE_ENTRY_NAMES")
    private String pageEntryNames;

    @DatabaseField(columnName = "IS_BOOK")
    private Boolean isBook;

    @DatabaseField(columnName = "FILE_SIZE")
    private Long fileSize;

    @Exclude
    @DatabaseField(columnName = "UNIQUE_CONTENT_ID")
    private Boolean isUniqueContentID;

    public BookArchive() {
    }

    @Override
    public VolumeItem createVolumeItem(List<BookChapter> chapters, History history, List<History> historyList,
                                       boolean isSingleMode, boolean archiveMode, boolean withChapters, boolean includeFileInfo) {
        VolumeItem volumeItem = new VolumeItem();
        volumeItem.setId(getContentId());
        volumeItem.setTitle(isSingleMode ? getTitle() : getVolumeTitle(archiveMode));
        if (!StringUtils.equalsIgnoreCase(volumeItem.getTitle(), getTitle())) {
            volumeItem.setAdditionalTitle(getTitle());
        }
        volumeItem.setYear(getYear());
        volumeItem.setCoverAccent(getCoverAccent());
        volumeItem.setCreatedAt(getCreatedAt());
        volumeItem.setBook(isBook());
        volumeItem.setVolume(volume);
        volumeItem.setPagesCount(getPagesCount());
        volumeItem.setHistory(history);

        if (withChapters && ArrayUtils.isNotEmpty(chapters)) {
            volumeItem.setChapters(fillChaptersHistory(
                    chapters.stream()
                            .filter(chapter -> StringUtils.equalsIgnoreCase(getContentId(), chapter.getArchiveId()))
                            .collect(Collectors.toList()),
                    historyList)
            );
        }

        if (includeFileInfo) {
            volumeItem.setFileName(FileUtils.getFileNameWithExt(getFolder(), true));
            volumeItem.setFilePath(getFolder());
        }
        return volumeItem;
    }

    private String getVolumeTitle(boolean archiveMode) {
        if (getVolume() < 0) {
            return getContentType() != ContentType.DOUJINSHI
                    ? FileUtils.getFileName(getFolder())
                    : getTitle();
        } else {
            String number = TypeUtils.isTrailingSignificant(getVolume())
                    ? String.format(getContentType() != ContentType.COMICS ? "%04.1f" : "%05.1f", getVolume()).replace(",", ".")
                    : String.format(getContentType() != ContentType.COMICS ? "%02d" : "%03d", getVolume().intValue());
            return String.format(
                    Localization.getFormatterForVolumeOrIssue(getContentType(), archiveMode),
                    getTitle(),
                    number
            );
        }
    }

    private Collection<BookChapter> fillChaptersHistory(List<BookChapter> chapters, List<History> historyList) {
        for (BookChapter chapter : chapters) {
            for (History chapterHistory : historyList) {
                if (StringUtils.equalsIgnoreCase(chapter.getChapterId(), chapterHistory.getArchiveHash())) {
                    chapter.setHistory(chapterHistory);
                    break;
                }
            }
        }
        return chapters;
    }

    public void copyFromBaseBook(BaseBook book) {
        this.title = book.getTitle();
        this.volume = Optional.ofNullable(book.getVolume())
                .filter(volume -> volume >= 0)
                .orElse(this.volume);
        super.fromBaseBook(book);
    }

    @Override
    public void fromBaseBook(BaseBook book) {
        this.mangaId = book.getContentId();
        this.title = book.getTitle();
        if (book.getVolume() >= 0) {
            this.volume = book.getVolume();
        }
        setFolder(book.getFolder());

        File file = new File(book.getFolder());
        if (FileUtils.isFile(file)) {
            setFileSize(file.length());
        }

        if (book instanceof BookArchive bookArchive) {
            setIsUniqueContentID(bookArchive.getIsUniqueContentID());
        }
        super.fromBaseBook(book);
    }

    @Override
    public BookSerie getSerie() {
        return (BookSerie) Optional.ofNullable(serie)
                .map(bookSerie -> AtsumeruApplication.getContext().getBean(BooksDaoManager.class).queryById(bookSerie.getDbId(), BookSerie.class))
                .orElse(null);
    }

    @Override
    public Long getSerieDbId() {
        return serie.getSerieDbId();
    }

    public Set<String> getChapterIds() {
        try {
            return new HashSet<>(
                    AtsumeruApplication.getContext()
                            .getBean(BooksDaoManager.class)
                            .getChaptersDao()
                            .queryRaw("select CHAPTER_ID from BOOK_CHAPTERS where ARCHIVE = " + getDbId(), (columnNames, resultColumns) -> resultColumns[0]).getResults()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public synchronized void setChapters(List<BookChapter> chapters) {
        try {
            synchronized (this) {
                if (ArrayUtils.isEmpty(this.chapters)) {
                    this.chapters = AtsumeruApplication.getContext()
                            .getBean(BooksDaoManager.class)
                            .getArchivesDao()
                            .getEmptyForeignCollection("CHAPTERS");
                } else {
                    this.chapters.clear();
                }

                this.chapters.addAll(chapters);
                setChaptersCount((long) chapters.size());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getContentId() {
        return mangaId;
    }

    @Override
    public void setContentId(String contentId) {
        mangaId = contentId;
    }

    @Override
    public String getContentLink() {
        return mangaLink;
    }

    @Override
    public void setContentLink(String contentLink) {
        mangaLink = contentLink;
    }

    @Override
    public String getContentLinks() {
        return mangaLinks;
    }

    @Override
    public void setContentLinks(String contentLinks) {
        mangaLinks = contentLinks;
    }

    @Override
    public Float getVolume() {
        return Optional.ofNullable(volume).orElse(-1f);
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
    }

    @Override
    public Integer getPagesCount() {
        return Optional.ofNullable(pagesCount).orElse(0);
    }

    @Override
    public Boolean isSingle() {
        return Optional.of(getStatus())
                .map(status -> status == Status.SINGLE)
                .orElse(false);
    }

    public Boolean isBook() {
        return Optional.ofNullable(isBook).orElse(false);
    }

    @Override
    public List<String> getPageEntryNames() {
        return ArrayUtils.splitString(pageEntryNames, ".\\|.");
    }

    public void setPageEntryNames(List<String> pageEntryNames) {
        this.pageEntryNames = StringUtils.join(".|.", pageEntryNames);
        setPagesCount(pageEntryNames.size());
    }

    public Long getFileSize() {
        return Optional.ofNullable(fileSize).orElse(-1L);
    }

    public Boolean getIsUniqueContentID() {
        return Optional.ofNullable(isUniqueContentID).orElse(false);
    }

    public boolean fileSizeChanged(File file) {
        return getFileSize() >= 0 && getFileSize() != file.length();
    }
}
