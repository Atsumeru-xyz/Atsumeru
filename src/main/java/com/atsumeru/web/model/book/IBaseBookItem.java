package com.atsumeru.web.model.book;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.enums.*;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.History;

import java.util.List;

public interface IBaseBookItem {
    VolumeItem createVolumeItem(List<BookChapter> chapters, History history, List<History> historyList,
                                boolean isSingleMode, boolean archiveMode, boolean withChapters, boolean includeFileInfo);
    Long getDbId();
    Long getSerieDbId();

    BookSerie getSerie();
    String getContentId();
    String getFolder();
    String getContentLink();
    String getContentLinks();
    String getTitle();
    String getAltTitle();
    String getJapTitle();
    String getKorTitle();
    String getCover();
    String getAuthors();
    String getArtists();
    String getTranslators();
    String getProducers();
    String getActors();
    String getScenarist();
    String getDubbing();
    String getStudio();
    String getPublisher();
    String getGenres();
    String getTags();
    String getYear();
    String getAired();
    String getCountry();
    String getLanguage();
    String getEvent();
    String getCharacters();
    String getParodies();
    String getCircles();
    String getMagazines();
    String getDescription();
    String getEpisodeLength();
    Float getVolume();
    Long getVolumesCount();
    String getScore();
    Integer getRating();
    Boolean getMature();
    Boolean getAdult();
    Boolean isSingle();

    Integer getPagesCount();

    Long getCreatedAt();
    Long getUpdatedAt();

    ContentType getContentType();
    Status getStatus();
    TranslationStatus getTranslationStatus();

    PlotType getPlotType();
    Censorship getCensorship();
    Color getColor();
    AgeRating getAgeRating();
    List<VolumeItem> getVolumes();

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
    void setProducers(String producers);
    void setActors(String producers);
    void setScenarist(String scenarist);
    void setDubbing(String dubbing);
    void setStudio(String studio);
    void setGenres(String genres);
    void setTags(String tags);
    void setYear(String year);
    void setAired(String aired);
    void setCountry(String country);
    void setLanguage(String language);
    void setDescription(String description);
    void setEpisodeLength(String episodeLength);
    void setVolume(float volume);
    void setVolumesCount(Long volumesCount);
    void setIsMature(Boolean isMature);
    void setIsAdult(Boolean isAdult);

    void setCreatedAt(Long timestamp);
    void setUpdatedAt(Long timestamp);

    void setStatus(String status);
    void setTranslationStatus(String translationStatus);
    void setPlotType(String plotType);
    void setCensorship(String censorship);
    void setVolumes(List<VolumeItem> volumes);

    void setCategories(String categories);
    String getCategories();

    Integer getUnreadedCount();
    Long getChaptersCount();
    Integer getNewChaptersCount();

    void setUnreadedCount(Integer value);
    void setChaptersCount(Long value);
    void setNewChaptersCount(Integer value);

    boolean isRemoved();
    void setRemoved(boolean removed);

    void addVolume(VolumeItem volumeItem);
    void addVolumes(List<VolumeItem> volumeItems);
}
