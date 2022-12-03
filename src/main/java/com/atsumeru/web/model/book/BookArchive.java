package com.atsumeru.web.model.book;

import com.atsumeru.web.util.GUString;
import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.enums.Status;
import com.atsumeru.web.json.adapter.LinksBidirectionalAdapter;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.util.GUType;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@DatabaseTable(tableName = "BOOK_ARCHIVES")
public class BookArchive extends BaseBook {
    transient String serieHash;
    @DatabaseField(foreign = true, columnName = "SERIE")
    private BookSerie serie;
    @ForeignCollectionField(columnName = "CHAPTERS")
    private ForeignCollection<BookChapter> chapters;
    @Expose
    @SerializedName("id")
    @DatabaseField(columnName = "MANGA_ID")
    private String mangaId;
    @Expose
    @SerializedName("link")
    @DatabaseField(columnName = "MANGA_LINK")
    private String mangaLink;
    @Expose
    @SerializedName("links")
    @DatabaseField(columnName = "MANGA_LINKS")
    @JsonAdapter(LinksBidirectionalAdapter.class)
    private String mangaLinks;
    @Expose
    @DatabaseField(columnName = "TITLE")
    private String title;
    @Expose
    @SerializedName("volume")
    @DatabaseField(columnName = "VOLUME")
    private Float volume;
    @DatabaseField(columnName = "PAGE_ENTRY_NAMES")
    private String pageEntryNames;
    @DatabaseField(columnName = "IS_BOOK")
    private Boolean isBook;
    @DatabaseField(columnName = "FILE_SIZE")
    private Long fileSize;
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
        if (!GUString.equalsIgnoreCase(volumeItem.getTitle(), getTitle())) {
            volumeItem.setAdditionalTitle(getTitle());
        }
        volumeItem.setYear(getYear());
        volumeItem.setCoverAccent(getCoverAccent());
        volumeItem.setCreatedAt(getCreatedAt());
        volumeItem.setBook(isBook());
        volumeItem.setVolume(volume);
        volumeItem.setPagesCount(getPagesCount());
        volumeItem.setHistory(history);

        if (withChapters && GUArray.isNotEmpty(chapters)) {
            volumeItem.setChapters(fillChaptersHistory(
                    chapters.stream()
                            .filter(chapter -> GUString.equalsIgnoreCase(getContentId(), chapter.getArchiveId()))
                            .collect(Collectors.toList()),
                    historyList)
            );
        }

        if (includeFileInfo) {
            volumeItem.setFileName(GUFile.getFileNameWithExt(getFolder(), true));
            volumeItem.setFilePath(getFolder());
        }
        return volumeItem;
    }

    private String getVolumeTitle(boolean archiveMode) {
        if (getVolume() < 0) {
            return getContentType() != ContentType.DOUJINSHI
                    ? GUFile.getFileName(getFolder())
                    : getTitle();
        } else {
            String number = GUType.isTrailingSignificant(getVolume())
                    ? String.format(getContentType() != ContentType.COMICS ? "%04.1f" : "%05.1f", getVolume()).replace(",", ".")
                    : String.format(getContentType() != ContentType.COMICS ? "%02d" : "%03d", getVolume().intValue());
            return String.format(
                    Localizr.getFormatterForVolumeOrIssue(getContentType(), archiveMode),
                    getTitle(),
                    number
            );
        }
    }

    private Collection<BookChapter> fillChaptersHistory(List<BookChapter> chapters, List<History> historyList) {
        for (BookChapter chapter : chapters) {
            for (History chapterHistory : historyList) {
                if (GUString.equalsIgnoreCase(chapter.getChapterId(), chapterHistory.getArchiveHash())) {
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
        if (GUFile.isFile(file)) {
            setFileSize(file.length());
        }

        if (book instanceof BookArchive) {
            setIsUniqueContentID(((BookArchive) book).getIsUniqueContentID());
        }
        super.fromBaseBook(book);
    }

    @Override
    public BookSerie getSerie() {
        return (BookSerie) Optional.ofNullable(serie)
                .map(bookSerie -> BooksDatabaseRepository.getInstance().getDaoManager().queryById(bookSerie.getDbId(), BookSerie.class))
                .orElse(null);
    }

    @Override
    public Long getSerieDbId() {
        return serie.getSerieDbId();
    }

    public Set<String> getChapterIds() {
        try {
            return new HashSet<>(BooksDatabaseRepository.getInstance()
                    .getDaoManager()
                    .getChaptersDao()
                    .queryRaw("select CHAPTER_ID from BOOK_CHAPTERS where ARCHIVE = " + getDbId(), (columnNames, resultColumns) -> resultColumns[0]).getResults());
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public synchronized void setChapters(List<BookChapter> chapters) {
        try {
            synchronized (this) {
                if (GUArray.isEmpty(this.chapters)) {
                    this.chapters = BooksDatabaseRepository.getInstance()
                            .getDaoManager()
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
        return GUArray.splitString(pageEntryNames, ".\\|.");
    }

    public void setPageEntryNames(List<String> pageEntryNames) {
        this.pageEntryNames = GUString.join(".|.", pageEntryNames);
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
