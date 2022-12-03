package com.atsumeru.web.model.book;

import com.atsumeru.web.enums.ServiceType;
import com.atsumeru.web.json.adapter.LinksBidirectionalAdapter;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.service.BoundService;
import com.atsumeru.web.model.book.volume.VolumeItem;
import com.atsumeru.web.model.database.DatabaseFields;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUString;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import kotlin.NotImplementedError;
import kotlin.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@EqualsAndHashCode(callSuper = true)
@DatabaseTable(tableName = "BOOK_SERIES")
public class BookSerie extends BaseBook {
    @Expose
    @SerializedName("id")
    @DatabaseField(columnName = "SERIE_ID")
    private String serieId;

    @Expose
    @SerializedName("link")
    @DatabaseField(columnName = "SERIE_LINK")
    private String serieLink;

    @Expose
    @SerializedName("links")
    @DatabaseField(columnName = "SERIE_LINKS")
    @JsonAdapter(LinksBidirectionalAdapter.class)
    private String serieLinks;

    @Expose
    @SerializedName("title")
    @DatabaseField(columnName = "TITLE")
    private String serieTitle;

    @Expose
    @SerializedName("is_single")
    @DatabaseField(columnName = "IS_SINGLE")
    private Boolean isSingle;

    // Bound Services IDs
    @DatabaseField(columnName = DatabaseFields.MAL_ID)
    private String malId;

    @DatabaseField(columnName = DatabaseFields.SHIKIMORI_ID)
    private String shikimoriId;

    @DatabaseField(columnName = DatabaseFields.KITSU_ID)
    private String kitsuId;

    @DatabaseField(columnName = DatabaseFields.ANILIST_ID)
    private String aniListId;

    @DatabaseField(columnName = DatabaseFields.MANGAUPDATES_ID)
    private String mangaUpdatesID;

    @DatabaseField(columnName = DatabaseFields.ANIMEPLANET_ID)
    private String animePlanetId;

    @DatabaseField(columnName = DatabaseFields.COMICVINE_ID)
    private String comicVineId;

    @DatabaseField(columnName = DatabaseFields.COMICSDB_ID)
    private String comicsDBId;

    @DatabaseField(columnName = DatabaseFields.HENTAG_ID)
    private String hentagId;

    @Expose
    @Getter
    @Setter
    private Float volume = null;

    @Getter
    @Setter
    private transient boolean isSerieUpdatedInImport;

    @Getter
    @Setter
    private transient boolean isReimportIfExist;

    public BookSerie() {
    }

    public VolumeItem createVolumeItem(List<BookChapter> chapters, History history, List<History> historyList,
                                       boolean isSingleMode, boolean archiveMode, boolean withChapters, boolean includeFileInfo) {
        throw new NotImplementedError("Can't create volume item from Serie");
    }

    public void copyFromBaseBook(BaseBook book) {
        this.serieTitle = book.getTitle();
        super.fromBaseBook(book);
    }

    @Override
    public void fromBaseBook(BaseBook book) {
        this.serieId = book.getContentId();
        this.serieTitle = book.getTitle();
        super.fromBaseBook(book);
    }

    public void prepareBoundServices() {
        boundServices = Optional.of(
                Stream.of(
                        new Pair<>(ServiceType.MYANIMELIST, malId),
                        new Pair<>(ServiceType.SHIKIMORI, shikimoriId),
                        new Pair<>(ServiceType.KITSU, kitsuId),
                        new Pair<>(ServiceType.ANILIST, aniListId),
                        new Pair<>(ServiceType.MANGAUPDATES, mangaUpdatesID),
                        new Pair<>(ServiceType.ANIMEPLANET, animePlanetId),
                        new Pair<>(ServiceType.COMICVINE, comicVineId),
                        new Pair<>(ServiceType.COMICSDB, comicsDBId),
                        new Pair<>(ServiceType.HENTAG, hentagId)
                )
                        .filter(pair -> GUString.isNotEmpty(pair.getSecond()))
                        .map(pair -> new BoundService(pair.getFirst(), pair.getSecond()))
                        .collect(Collectors.toList()))
                .filter(GUArray::isNotEmpty)
                .orElse(null);
    }

    public void fromBoundServicesToIds() {
        if (GUArray.isNotEmpty(boundServices)) {
            resetBoundServicesIds();
            boundServices.forEach(service -> {
                String id = service.getId();
                switch (service.getServiceType()) {
                    case MYANIMELIST:
                        malId = id;
                        break;
                    case SHIKIMORI:
                        shikimoriId = id;
                        break;
                    case KITSU:
                        kitsuId = id;
                        break;
                    case ANILIST:
                        aniListId = id;
                        break;
                    case MANGAUPDATES:
                        mangaUpdatesID = id;
                        break;
                    case ANIMEPLANET:
                        animePlanetId = id;
                        break;
                    case COMICVINE:
                        comicVineId = id;
                        break;
                    case COMICSDB:
                        comicsDBId = id;
                        break;
                    case HENTAG:
                        hentagId = id;
                        break;
                }
            });
        }
    }

    private void resetBoundServicesIds() {
        malId = null;
        shikimoriId = null;
        kitsuId = null;
        aniListId = null;
        mangaUpdatesID = null;
        animePlanetId = null;
        comicVineId = null;
        comicsDBId = null;
        hentagId = null;
    }

    @Override
    public void setContentId(String contentId) {
        serieId = contentId;
    }

    @Override
    public String getContentId() {
        return serieId;
    }

    @Override
    public String getContentLink() {
        return serieLink;
    }

    @Override
    public String getTitle() {
        return serieTitle;
    }

    @Override
    public void setTitle(String serieTitle) {
        this.serieTitle = serieTitle;
    }

    @Override
    public BookSerie getSerie() {
        return this;
    }

    @Override
    public void setSerie(BookSerie serie) {
        // stub
    }

    @Override
    public Long getSerieDbId() {
        return getDbId();
    }

    @Override
    public void setContentLink(String contentLink) {
        serieLink = contentLink;
    }

    @Override
    public String getContentLinks() {
        return serieLinks;
    }

    @Override
    public void setContentLinks(String contentLinks) {
        serieLinks = contentLinks;
    }

    @Override
    public Boolean isSingle() {
        return Optional.ofNullable(isSingle).orElse(false);
    }

    @Override
    public List<String> getPageEntryNames() {
        return new ArrayList<>();
    }
}
