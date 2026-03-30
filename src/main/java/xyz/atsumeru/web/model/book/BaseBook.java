package xyz.atsumeru.web.model.book;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Setter;
import xyz.atsumeru.web.enums.*;
import xyz.atsumeru.web.json.adapter.AdminFieldAdapter;
import xyz.atsumeru.web.json.adapter.CategoriesFieldAdapter;
import xyz.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.model.book.service.BoundService;
import xyz.atsumeru.web.model.book.volume.VolumeItem;
import xyz.atsumeru.web.util.EnumUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public abstract class BaseBook implements IBaseBookItem {
    @Exclude
    @Schema(name = "_id")
    @SerializedName("_id")
    @DatabaseField(generatedId = true)
    private Long id;

    @Schema(name = "alt_title")
    @SerializedName("alt_title")
    @DatabaseField(columnName = "ALTERNATIVE_TITLE")
    private String altTitle;

    @Schema(name = "jap_title")
    @SerializedName("jap_title")
    @DatabaseField(columnName = "JAP_TITLE")
    private String japTitle;

    @Schema(name = "kor_title")
    @SerializedName("kor_title")
    @DatabaseField(columnName = "KOR_TITLE")
    private String korTitle;

    @JsonAdapter(AdminFieldAdapter.class)
    @DatabaseField(columnName = "FOLDER")
    private String folder;

    @DatabaseField(columnName = "COVER")
    private String cover;

    @DatabaseField(columnName = "PUBLISHER")
    private String publisher;

    @DatabaseField(columnName = "AUTHORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String authors;

    @DatabaseField(columnName = "ARTISTS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String artists;

    @DatabaseField(columnName = "TRANSLATORS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String translators;

    @DatabaseField(columnName = "MAGAZINES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String magazines;

    @DatabaseField(columnName = "GENRES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String genres;

    @DatabaseField(columnName = "TAGS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String tags;

    @DatabaseField(columnName = "YEAR")
    private String year;

    @DatabaseField(columnName = "COUNTRY")
    private String country;

    @DatabaseField(columnName = "LANGUAGE")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    @Schema(name = "languages")
    @SerializedName("languages")
    private String language;

    @Schema(name = "content_type")
    @SerializedName("content_type")
    @DatabaseField(columnName = "CONTENT_TYPE")
    private String contentType;

    @DatabaseField(columnName = "DESCRIPTION")
    private String description;

    @DatabaseField(columnName = "RELATED")
    private String related;

    @DatabaseField(columnName = "EVENT")
    private String event;

    @Schema(name = "is_mature")
    @SerializedName("is_mature")
    @DatabaseField(columnName = "IS_MATURE")
    private Boolean isMature;

    @Schema(name = "is_adult")
    @SerializedName("is_adult")
    @DatabaseField(columnName = "IS_ADULT")
    private Boolean isAdult;

    @Schema(name = "volumes_count")
    @SerializedName("volumes_count")
    @DatabaseField(columnName = "VOLUMES_COUNT")
    private Long volumesCount;

    @Schema(name = "chapters_count")
    @SerializedName("chapters_count")
    @DatabaseField(columnName = "CHAPTERS_COUNT")
    private Long chaptersCount;

    @DatabaseField(columnName = "PAGES_COUNT")
    protected Integer pagesCount;

    @DatabaseField(columnName = "STATUS")
    private String status;

    @Schema(name = "translation_status")
    @SerializedName("translation_status")
    @DatabaseField(columnName = "TRANSLATION_STATUS")
    private String translationStatus;

    @Schema(name = "plot_type")
    @SerializedName("plot_type")
    @DatabaseField(columnName = "PLOT_TYPE")
    private String plotType;

    @DatabaseField(columnName = "CENSORSHIP")
    private String censorship;

    @DatabaseField(columnName = "SERIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String series;

    @DatabaseField(columnName = "PARODIES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String parodies;

    @DatabaseField(columnName = "CIRCLES")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String circles;

    @DatabaseField(columnName = "CHARACTERS")
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String characters;

    @DatabaseField(columnName = "COLOR")
    private String color;

    @DatabaseField(columnName = "RATING")
    private Integer rating;

    @DatabaseField(columnName = "SCORE")
    private String score;

    @Schema(name = "cover_accent")
    @SerializedName("cover_accent")
    @DatabaseField(columnName = "COVER_ACCENT")
    private String coverAccent;

    @Schema(name = "created_at")
    @SerializedName("created_at")
    @DatabaseField(columnName = "CREATED_AT")
    private Long createdAt;

    @Schema(name = "updated_at")
    @SerializedName("updated_at")
    @DatabaseField(columnName = "UPDATED_AT")
    private Long updatedAt;

    @JsonAdapter(CategoriesFieldAdapter.class)
    @DatabaseField(columnName = "CATEGORIES")
    private String categories;

    @Exclude
    @DatabaseField(columnName = "REMOVED", defaultValue = "false")
    private boolean removed;

    @Setter
    @Schema(name = "bound_services")
    @SerializedName("bound_services")
    protected List<BoundService> boundServices;

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
    public Long getVolumesCount() {
        return Optional.ofNullable(volumesCount).orElse(0L);
    }

    @Override
    public Long getChaptersCount() {
        return Optional.ofNullable(chaptersCount).orElse(0L);
    }

    @Override
    public Boolean getIsMature() {
        return Optional.ofNullable(isMature).orElse(false);
    }

    @Override
    public Boolean getIsAdult() {
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
        return EnumUtils.valueOf(ContentType.class, contentType);
    }

    @Override
    public Status getStatus() {
        return EnumUtils.valueOf(Status.class, status);
    }

    @Override
    public TranslationStatus getTranslationStatus() {
        return EnumUtils.valueOf(TranslationStatus.class, translationStatus);
    }

    @Override
    public PlotType getPlotType() {
        return EnumUtils.valueOf(PlotType.class, plotType);
    }

    @Override
    public Censorship getCensorship() {
        return EnumUtils.valueOf(Censorship.class, censorship);
    }

    @Override
    public Color getColor() {
        return EnumUtils.valueOf(Color.class, color);
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
        if (StringUtils.isEmpty(coverAccent)) {
            coverAccent = accent;
        }
    }

    public boolean notRemoved() {
        return !removed;
    }

    // Stub
    @Override
    public Float getVolume() {
        return -1f;
    }

    @Override
    public void setVolume(float volume) {
        // stub
    }
}
