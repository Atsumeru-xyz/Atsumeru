package xyz.atsumeru.web.model.book;

import io.swagger.v3.oas.annotations.media.Schema;
import xyz.atsumeru.web.enums.*;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.book.volume.VolumeItem;
import xyz.atsumeru.web.model.database.History;

import java.util.List;

public interface IBaseBookItem {
    VolumeItem createVolumeItem(List<BookChapter> chapters, History history, List<History> historyList,
                                boolean isSingleMode, boolean archiveMode, boolean withChapters, boolean includeFileInfo);
    @Schema(name = "db_id")
    Long getDbId();
    @Schema(name = "serie_db_id")
    Long getSerieDbId();

    BookSerie getSerie();
    @Schema(name = "id")
    String getContentId();
    String getFolder();
    @Schema(name = "content_link")
    String getContentLink();
    @Schema(name = "content_links")
    String getContentLinks();
    String getTitle();
    @Schema(name = "alt_title")
    String getAltTitle();
    @Schema(name = "jap_title")
    String getJapTitle();
    @Schema(name = "kor_title")
    String getKorTitle();
    String getCover();
    String getAuthors();
    String getArtists();
    String getTranslators();
    String getPublisher();
    String getGenres();
    String getTags();
    String getYear();
    String getCountry();
    String getLanguage();
    String getEvent();
    String getCharacters();
    String getSeries();
    String getParodies();
    String getCircles();
    String getMagazines();
    String getDescription();
    Float getVolume();
    @Schema(name = "volumes_count")
    Long getVolumesCount();
    String getScore();
    Integer getRating();
    @Schema(name = "is_mature")
    Boolean getIsMature();
    @Schema(name = "is_adult")
    Boolean getIsAdult();
    Boolean isSingle();

    @Schema(name = "pages_count")
    Integer getPagesCount();

    @Schema(name = "created_at")
    Long getCreatedAt();
    @Schema(name = "updated_at")
    Long getUpdatedAt();

    @Schema(name = "content_type")
    ContentType getContentType();
    Status getStatus();
    TranslationStatus getTranslationStatus();

    @Schema(name = "plot_type")
    PlotType getPlotType();
    Censorship getCensorship();
    Color getColor();
    @Schema(name = "age_rating")
    AgeRating getAgeRating();
    List<VolumeItem> getVolumes();

    @Schema(name = "page_entry_names")
    List<String> getPageEntryNames();

    void setSerie(BookSerie serie);
    void setContentId(String contentId);
    void setFolder(String folder);
    void setContentLink(String contentLink);
    void setContentLinks(String contentLinks);
    void setContentType(String contentType);
    void setTitle(String serieTitle);
    void setAltTitle(String alternativeTitle);
    void setJapTitle(String japTitle);
    void setKorTitle(String korTitle);
    void setCover(String cover);
    void setAuthors(String authors);
    void setTranslators(String translators);
    void setGenres(String genres);
    void setTags(String tags);
    void setYear(String year);
    void setCountry(String country);
    void setLanguage(String language);
    void setDescription(String description);
    void setVolume(float volume);
    void setVolumesCount(Long volumesCount);
    void setIsMature(Boolean isMature);
    void setIsAdult(Boolean isAdult);

    void setCreatedAt(Long timestamp);
    void setUpdatedAt(Long timestamp);

    void setStatus(String status);
    @Schema(name = "translation_status")
    void setTranslationStatus(String translationStatus);
    void setPlotType(String plotType);
    void setCensorship(String censorship);
    void setVolumes(List<VolumeItem> volumes);

    void setCategories(String categories);
    String getCategories();

    @Schema(name = "chapters_count")
    Long getChaptersCount();

    void setChaptersCount(Long value);

    boolean isRemoved();
    void setRemoved(boolean removed);

    void addVolume(VolumeItem volumeItem);
    void addVolumes(List<VolumeItem> volumeItems);
}
