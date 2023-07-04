package com.atsumeru.web.model.book;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.enums.*;
import com.atsumeru.web.model.book.service.BoundService;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.enums.Genre;
import com.atsumeru.web.util.GUEnum;
import com.atsumeru.web.json.adapter.AdminFieldAdapter;
import com.atsumeru.web.json.adapter.CategoriesFieldAdapter;
import com.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public abstract class BaseBook implements IBaseBookItem {
    @DatabaseField(generatedId = true)
    private Long id;

    @Expose
    @SerializedName("alt_title")
    @DatabaseField(columnName = "ALTERNATIVE_TITLE")
    private String altTitle;

    @Expose
    @SerializedName("jap_title")
    @DatabaseField(columnName = "JAP_TITLE")
    private String japTitle;

    @Expose
    @SerializedName("kor_title")
    @DatabaseField(columnName = "KOR_TITLE")
    private String korTitle;

    @Expose
    @JsonAdapter(AdminFieldAdapter.class)
    @DatabaseField(columnName = "FOLDER")
    private String folder;

    @Expose
    @DatabaseField(columnName = "COVER")
    private String cover;

    @Expose
    @DatabaseField(columnName = "PUBLISHER")
    private String publisher;

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
    @DatabaseField(columnName = "MAGAZINES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String magazines;

    @Expose
    @DatabaseField(columnName = "GENRES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String genres;

    @Expose
    @DatabaseField(columnName = "TAGS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String tags;

    @Expose
    @DatabaseField(columnName = "YEAR")
    private String year;

    @Expose
    @DatabaseField(columnName = "COUNTRY")
    private String country;

    @Expose
    @DatabaseField(columnName = "LANGUAGE")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @SerializedName("languages")
    private String language;

    @Expose
    @SerializedName("content_type")
    @DatabaseField(columnName = "CONTENT_TYPE")
    private String contentType;

    @Expose
    @DatabaseField(columnName = "DESCRIPTION")
    private String description;

    @Expose
    @DatabaseField(columnName = "RELATED")
    private String related;

    @Expose
    @DatabaseField(columnName = "EVENT")
    private String event;

    @Expose
    @SerializedName("is_mature")
    @DatabaseField(columnName = "IS_MATURE")
    private Boolean isMature;

    @Expose
    @SerializedName("is_adult")
    @DatabaseField(columnName = "IS_ADULT")
    private Boolean isAdult;

    @Expose
    @SerializedName("volumes_count")
    @DatabaseField(columnName = "VOLUMES_COUNT")
    private Long volumesCount;

    @Expose
    @SerializedName("chapters_count")
    @DatabaseField(columnName = "CHAPTERS_COUNT")
    private Long chaptersCount;

    @DatabaseField(columnName = "PAGES_COUNT")
    protected Integer pagesCount;

    @DatabaseField(columnName = "NEW_CHAPTERS_COUNT")
    private Integer newChaptersCount;

    @DatabaseField(columnName = "UNREADED_COUNT")
    private Integer unreadedCount;

    @Expose
    @DatabaseField(columnName = "STATUS")
    private String status;

    @Expose
    @SerializedName("translation_status")
    @DatabaseField(columnName = "TRANSLATION_STATUS")
    private String translationStatus;

    @Expose
    @SerializedName("plot_type")
    @DatabaseField(columnName = "PLOT_TYPE")
    private String plotType;

    @Expose
    @DatabaseField(columnName = "CENSORSHIP")
    private String censorship;

    @Expose
    @DatabaseField(columnName = "SERIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String series;

    @Expose
    @DatabaseField(columnName = "PARODIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String parodies;

    @Expose
    @DatabaseField(columnName = "CIRCLES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String circles;

    @Expose
    @DatabaseField(columnName = "CHARACTERS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String characters;

    @Expose
    @DatabaseField(columnName = "COLOR")
    private String color;

    @Expose
    @DatabaseField(columnName = "RATING")
    private Integer rating;

    @Expose
    @DatabaseField(columnName = "SCORE")
    private String score;

    @Expose
    @SerializedName("cover_accent")
    @DatabaseField(columnName = "COVER_ACCENT")
    private String coverAccent;

    @Expose
    @SerializedName("created_at")
    @DatabaseField(columnName = "CREATED_AT")
    private Long createdAt;

    @Expose
    @SerializedName("updated_at")
    @DatabaseField(columnName = "UPDATED_AT")
    private Long updatedAt;

    @Expose
    @JsonAdapter(CategoriesFieldAdapter.class)
    @DatabaseField(columnName = "CATEGORIES")
    private String categories;

    @DatabaseField(columnName = "REMOVED", defaultValue = "false")
    private boolean removed;

    @Setter
    @Expose
    @SerializedName("bound_services")
    protected List<BoundService> boundServices;

    @Expose
    private List<VolumeItem> volumes;

    public BaseBook() {
    }

    protected void fromBaseBook(BaseBook book) {
        setTimestamps();

        setContentLink(book.getContentLink());
        setContentLinks(book.getContentLinks());
        this.altTitle = book.getAltTitle();
        this.japTitle = book.getJapTitle();
        this.korTitle = book.getKorTitle();
        this.description = book.getDescription();
        this.related = book.getRelated();
        this.event = book.getEvent();

        this.publisher = book.getPublisher();
        this.authors = book.getAuthors();
        this.artists = book.getArtists();
        this.translators = book.getTranslators();
        setGenres(book.getGenres());
        this.tags = book.getTags();
        this.year = book.getYear();
        this.country = book.getCountry();
        this.language = book.getLanguage();
        this.series = book.getSeries();
        this.parodies = book.getParodies();
        this.circles = book.getCircles();
        this.magazines = book.getMagazines();
        this.characters = book.getCharacters();
        this.contentType = book.getContentType() != null ? book.getContentType().toString() : null;
        this.isMature = book.getIsMature();
        this.isAdult = book.getIsAdult();
        this.boundServices = book.getBoundServices();

        setEnumsAndScores(book.getStatus(),
                book.getTranslationStatus(),
                book.getPlotType(),
                book.getCensorship(),
                book.getColor(),
                book.getRating(),
                book.getScore());

        setCoverAccent(book.getCoverAccent());
    }

    private void setEnumsAndScores(Status status, TranslationStatus translationStatus, PlotType plotType, Censorship censorship, Color color, Integer rating, String score) {
        this.status = Optional.ofNullable(status).map(Enum::toString).orElse(null);
        this.translationStatus = Optional.ofNullable(translationStatus).map(Enum::toString).orElse(null);
        this.plotType = Optional.ofNullable(plotType).map(Enum::toString).orElse(null);
        this.censorship = Optional.ofNullable(censorship).map(Enum::toString).orElse(null);
        this.color = Optional.ofNullable(color).map(Enum::toString).orElse(null);

        this.rating = rating;
        this.score = score;
    }

    public void updateTimestamps() {
        setTimestamps();
    }

    protected void setTimestamps() {
        long currentTime = System.currentTimeMillis();
        if (id == null) {
            createdAt = currentTime;
        }
        updatedAt = currentTime;
    }

    public void genresToLine(List<Genre> values) {
        this.genres = values
                .stream()
                .map(Enum::ordinal)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public ContentType getMangaContentType() {
        return Optional.ofNullable(contentType)
                .map(value -> ContentType.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    public Status getMangaStatus() {
        return Optional.ofNullable(status)
                .map(value -> Status.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    public TranslationStatus getMangaTranslationStatus() {
        return Optional.ofNullable(translationStatus)
                .map(value -> TranslationStatus.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    public PlotType getMangaPlotType() {
        return Optional.ofNullable(plotType)
                .map(value -> PlotType.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    public Censorship getMangaCensorship() {
        return Optional.ofNullable(censorship)
                .map(value -> Censorship.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    public Color getMangaColor() {
        return Optional.ofNullable(color)
                .map(value -> Color.valueOf(value.toUpperCase()))
                .orElse(null);
    }

    @Override
    public Long getDbId() {
        return this.id;
    }

    @Override
    public Integer getUnreadedCount() {
        return Optional.ofNullable(unreadedCount).orElse(0);
    }

    @Override
    public Long getVolumesCount() {
        return Optional.ofNullable(volumesCount).orElse(0L);
    }

    @Override
    public Long getChaptersCount() {
        return Optional.ofNullable(chaptersCount).orElse(0L);
    }

    @Override
    public Integer getNewChaptersCount() {
        return Optional.ofNullable(newChaptersCount).orElse(0);
    }

    @Override
    public Boolean getMature() {
        return Optional.ofNullable(isMature).orElse(false);
    }

    @Override
    public Boolean getAdult() {
        return Optional.ofNullable(isAdult).orElse(false);
    }

    @Override
    public Integer getRating() {
        return Optional.ofNullable(rating).orElse(0);
    }

    @Override
    public AgeRating getAgeRating() {
        if (getIsAdult()) {
            return AgeRating.ADULTS_ONLY;
        } else if (getIsMature()) {
            return AgeRating.MATURE;
        } else {
            return AgeRating.EVERYONE;
        }
    }

    @Override
    public ContentType getContentType() {
        return GUEnum.valueOf(ContentType.class, contentType);
    }

    @Override
    public Status getStatus() {
        return GUEnum.valueOf(Status.class, status);
    }

    @Override
    public TranslationStatus getTranslationStatus() {
        return GUEnum.valueOf(TranslationStatus.class, translationStatus);
    }

    @Override
    public PlotType getPlotType() {
        return GUEnum.valueOf(PlotType.class, plotType);
    }

    @Override
    public Censorship getCensorship() {
        return GUEnum.valueOf(Censorship.class, censorship);
    }

    @Override
    public Color getColor() {
        return GUEnum.valueOf(Color.class, color);
    }

    @Override
    public void addVolume(VolumeItem volumeItem) {
        if (volumes == null) {
            volumes = new ArrayList<>();
        }
        volumes.add(volumeItem);
    }

    @Override
    public void addVolumes(List<VolumeItem> volumeItems) {
        if (volumes == null) {
            volumes = new ArrayList<>();
        }
        volumes.addAll(volumeItems);
    }

    public void setCoverAccent(String accent) {
        if (GUString.isEmpty(coverAccent)) {
            coverAccent = accent;
        }
    }

    public boolean notRemoved() {
        return !removed;
    }

    // Stubs
    @Override
    public String getProducers() {
        return "";
    }

    @Override
    public String getActors() {
        return "";
    }

    @Override
    public String getScenarist() {
        return "";
    }

    @Override
    public String getDubbing() {
        return "";
    }

    @Override
    public String getStudio() {
        return "";
    }

    @Override
    public void setProducers(String producers) {
        // stub
    }

    @Override
    public void setActors(String actors) {
        // stub
    }

    @Override
    public void setScenarist(String scenarist) {
        // stub
    }

    @Override
    public void setDubbing(String dubbing) {
        // stub
    }

    @Override
    public void setStudio(String studio) {
        // stub
    }

    @Override
    public String getAired() {
        return "";
    }


    @Override
    public void setAired(String aired) {
        // stub
    }

    @Override
    public String getEpisodeLength() {
        return "";
    }

    @Override
    public void setEpisodeLength(String episodeLength) {
        // stub
    }

    @Override
    public Float getVolume() {
        return -1f;
    }

    @Override
    public void setVolume(float volume) {
        // stub
    }

    public boolean getIsMature() {
        return Optional.ofNullable(isMature).orElse(false);
    }

    public boolean getIsAdult() {
        return Optional.ofNullable(isAdult).orElse(false);
    }
}
