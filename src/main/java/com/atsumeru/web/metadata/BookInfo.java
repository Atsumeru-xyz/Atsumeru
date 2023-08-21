package com.atsumeru.web.metadata;

import com.atsumeru.web.enums.*;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.util.*;
import com.atsumeru.web.helper.JSONHelper;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.service.BoundService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.atsumeru.web.helper.JSONLogHelper.putJSON;

public class BookInfo {

    public static JSONObject toJSON(BookArchive archive, List<BoundService> boundServices) throws JSONException {
        JSONObject obj = new JSONObject();

        // Basic Metadata
        if (archive.getIsUniqueContentID()) {
            putHashes(obj, archive.getSerieHash(), archive.getContentId());
        }
        putJSON(obj, "link", archive.getContentLink());
        putLinks(obj, archive.getContentLinks());
        putJSON(obj, "cover", archive.getCover());

        // Titles
        putJSON(obj, "title", archive.getTitle());
        putJSON(obj, "alt_title", archive.getAltTitle());
        putJSON(obj, "jap_title", archive.getJapTitle());
        putJSON(obj, "kor_title", archive.getKorTitle());

        // Main info
        putJSON(obj, "country", archive.getCountry());
        putJSON(obj, "publisher", archive.getPublisher());
        putJSON(obj, "published", archive.getYear());
        putJSON(obj, "event", archive.getEvent());
        putJSON(obj, "description", archive.getDescription());

        // Info lists
        putJSON(obj, "authors", ArrayUtils.splitString(archive.getAuthors()));
        putJSON(obj, "artists", ArrayUtils.splitString(archive.getArtists()));
        putJSON(obj, "languages", ArrayUtils.splitString(archive.getLanguage()));
        putJSON(obj, "translators", ArrayUtils.splitString(archive.getTranslators()));
        putJSON(obj, "series", ArrayUtils.splitString(archive.getSeries()));
        putJSON(obj, "parodies", ArrayUtils.splitString(archive.getParodies()));
        putJSON(obj, "circles", ArrayUtils.splitString(archive.getCircles()));
        putJSON(obj, "magazines", ArrayUtils.splitString(archive.getMagazines()));
        putJSON(obj, "characters", ArrayUtils.splitString(archive.getCharacters()));

        // Volumes/Chapters
        putJSON(obj, "volume", archive.getVolume());
        putJSON(obj, "volumes", archive.getVolumesCount());
        putJSON(obj, "chapters", archive.getChaptersCount());

        // Genres/Tags
        putJSON(obj, "genres", ArrayUtils.splitString(archive.getGenres()));
        putJSON(obj, "tags", ArrayUtils.splitString(archive.getTags()));

        // Age Rating
        putJSON(obj, "age_rating", archive.getAgeRating());

        // Statuses
        putJSON(obj, "status", archive.getStatus());
        putJSON(obj, "translation_status", archive.getTranslationStatus());
        putJSON(obj, "plot_type", archive.getPlotType());
        putJSON(obj, "censorship", archive.getCensorship());
        putJSON(obj, "content_type", archive.getContentType());
        putJSON(obj, "color", archive.getColor());

        // Score
        putJSON(obj, "score", archive.getScore());
        putJSON(obj, "rating", archive.getRating());

        putBoundServices(obj, boundServices);

        return obj;
    }

    public static boolean fromJSON(BookArchive archive, String jsonStr) {
        JSONObject obj = JSONHelper.fromString(jsonStr);

        // Hashes
        JSONObject atsumeru = JSONHelper.getObjectSafe(obj, "atsumeru");
        if (atsumeru != null) {
            archive.setSerieHash(JSONHelper.getStringSafe(atsumeru, "serie_hash", Optional.ofNullable(archive.getSerie())
                    .map(BookSerie::getContentId)
                    .orElse(null)));
            archive.setContentId(JSONHelper.getStringSafe(atsumeru, "hash", archive.getContentId()));
        }

        // Basic Metadata
        archive.setIsUniqueContentID(StringUtils.isNotEmpty(archive.getContentId()));
        archive.setContentLink(JSONHelper.getStringSafe(obj, "link"));
        archive.setContentLinks(getLinks(obj));
        archive.setCover(JSONHelper.getStringSafe(obj, "cover"));

        // Titles
        archive.setTitle(JSONHelper.getStringSafe(obj, "title"));
        archive.setAltTitle(JSONHelper.getStringSafe(obj, "alt_title"));
        archive.setJapTitle(JSONHelper.getStringSafe(obj, "jap_title"));
        archive.setKorTitle(JSONHelper.getStringSafe(obj, "kor_title"));
        // TODO: add synonyms support
//        archive.setSynonyms(JSONHelper.getStringSafe(obj, "synonyms"));

        // Main info
        List<String> authors = JSONHelper.getStringList(obj, "authors");
        String author = JSONHelper.getStringSafe(obj, "author");
        if (StringUtils.isNotEmpty(author)) {
            authors.add(author);
        }

        archive.setAuthors(StringUtils.join(",", authors));
        archive.setCountry(JSONHelper.getStringSafe(obj, "country"));
        archive.setPublisher(JSONHelper.getStringSafe(obj, "publisher"));
        archive.setYear(JSONHelper.getStringSafe(obj, "published"));

        archive.setVolume(JSONHelper.getFloatSafe(obj, "volume", -1f));
        archive.setChaptersCount(getChaptersCount(obj));

        archive.setGenres(getGenres(obj));
        archive.setTags(StringUtils.join(",", JSONHelper.getStringList(obj, "tags")));
        archive.setArtists(StringUtils.join(",", JSONHelper.getStringList(obj, "artists")));
        archive.setTranslators(StringUtils.join(",", JSONHelper.getStringList(obj, "translators")));
        archive.setLanguage(StringUtils.join(",", JSONHelper.getStringList(obj, "languages")));
        archive.setSeries(StringUtils.join(",", JSONHelper.getStringList(obj, "series")));
        archive.setParodies(StringUtils.join(",", JSONHelper.getStringList(obj, "parodies")));
        archive.setCircles(StringUtils.join(",", JSONHelper.getStringList(obj, "circles")));
        archive.setMagazines(StringUtils.join(",", JSONHelper.getStringList(obj, "magazines")));
        archive.setCharacters(StringUtils.join(",", JSONHelper.getStringList(obj, "characters")));
        archive.setEvent(JSONHelper.getStringSafe(obj, "event"));
        archive.setDescription(JSONHelper.getStringSafe(obj, "description"));

        // Age Rating
        AgeRating ageRating = EnumUtils.valueOf(AgeRating.class, JSONHelper.getStringSafe(obj, "age_rating"));
        archive.setIsMature(ageRating == AgeRating.MATURE);
        archive.setIsAdult(ageRating == AgeRating.ADULTS_ONLY);

        // Statuses
        archive.setStatus(EnumUtils.valueOf(Status.class, JSONHelper.getStringSafe(obj, "status")).toString());
        archive.setTranslationStatus(EnumUtils.valueOf(TranslationStatus.class, JSONHelper.getStringSafe(obj, "translation_status")).toString());
        archive.setPlotType(EnumUtils.valueOf(PlotType.class, JSONHelper.getStringSafe(obj, "plot_type")).toString());
        archive.setCensorship(EnumUtils.valueOf(Censorship.class, JSONHelper.getStringSafe(obj, "censorship")).toString());
        archive.setContentType(EnumUtils.valueOf(ContentType.class, JSONHelper.getStringSafe(obj, "content_type")).toString());
        archive.setColor(EnumUtils.valueOf(Color.class, JSONHelper.getStringSafe(obj, "color")).toString());

        // Score
        archive.setScore(
                Optional.ofNullable(JSONHelper.getStringSafe(obj, "score"))
                        .orElse(
                                Optional.ofNullable(JSONHelper.getFloatSafe(obj, "rating", null))
                                        .map(String::valueOf)
                                        .orElse(null)
                        )
        );
        archive.setRating(JSONHelper.getIntSafe(obj, "rating", 0));

        // BoundServices
        archive.setBoundServices(getBoundServices(obj));

        return true;
    }

    public static JSONObject toJson(BookChapter chapter) {
        JSONObject obj = new JSONObject();

        // Basic Metadata
        putHashes(obj, null, chapter.getChapterId());

        putJSON(obj, "title", chapter.getTitle());
        putJSON(obj, "alt_title", chapter.getAltTitle());

        putJSON(obj, "chapter", chapter.getChapter());
        putJSON(obj, "description", chapter.getDescription());

        // Info lists
        putJSON(obj, "authors", ArrayUtils.splitString(chapter.getAuthors()));
        putJSON(obj, "artists", ArrayUtils.splitString(chapter.getArtists()));
        putJSON(obj, "languages", ArrayUtils.splitString(chapter.getLanguage()));
        putJSON(obj, "translators", ArrayUtils.splitString(chapter.getTranslators()));
        putJSON(obj, "parodies", ArrayUtils.splitString(chapter.getParodies()));
        putJSON(obj, "characters", ArrayUtils.splitString(chapter.getCharacters()));

        // Genres/Tags
        putJSON(obj, "genres", ArrayUtils.splitString(chapter.getGenres()));
        putJSON(obj, "tags", ArrayUtils.splitString(chapter.getTags()));

        // Statuses
        putJSON(obj, "censorship", chapter.getCensorship());
        putJSON(obj, "color", chapter.getColor());

        return obj;
    }

    public static void fromJSON(BookChapter chapter, String json, String chapterFolder, String archivePath, String archiveHash) {
        JSONObject obj = JSONHelper.fromString(json);

        chapter.setTitle(
                NotEmptyString.ofNullable(JSONHelper.getStringSafe(obj, "title"))
                        .orElse(NotEmptyString.ofNullable(chapterFolder)
                                .apply(FileUtils::getDirName)
                                .orElse(FileUtils.getFileName(archivePath)))
        );
        chapter.setAltTitle(JSONHelper.getStringSafe(obj, "alt_title"));

        // Hashes
        chapter.setChapterId(
                Optional.ofNullable(JSONHelper.getObjectSafe(obj, "atsumeru"))
                        .map(atsumeruObj ->
                                Optional.ofNullable(JSONHelper.getStringSafe(atsumeruObj, "hash"))
                                        .orElseGet(() -> StringUtils.md5Hex(archiveHash + chapter.getTitle()))
                        )
                        .orElse(StringUtils.md5Hex(archiveHash + chapter.getTitle()))
        );

        chapter.setFolder(chapterFolder);

        chapter.setAuthors(StringUtils.join(",", JSONHelper.getStringList(obj, "authors")));
        chapter.setArtists(StringUtils.join(",", JSONHelper.getStringList(obj, "artists")));
        chapter.setTranslators(StringUtils.join(",", JSONHelper.getStringList(obj, "translators")));
        chapter.setLanguage(StringUtils.join(",", JSONHelper.getStringList(obj, "languages")));
        chapter.setParodies(StringUtils.join(",", JSONHelper.getStringList(obj, "parodies")));
        chapter.setCharacters(StringUtils.join(",", JSONHelper.getStringList(obj, "characters")));

        chapter.setCensorship(EnumUtils.valueOf(Censorship.class, JSONHelper.getStringSafe(obj, "censorship")).toString());
        chapter.setColor(EnumUtils.valueOf(Color.class, JSONHelper.getStringSafe(obj, "color")).toString());

        chapter.setDescription(JSONHelper.getStringSafe(obj, "description"));

        chapter.setGenres(getGenres(obj));
        chapter.setTags(StringUtils.join(",", JSONHelper.getStringList(obj, "tags")));
    }

    private static String getGenres(JSONObject obj) {
        try {
            List<String> genreOrdinals = JSONHelper.getStringList(obj, "genres");
            if (ArrayUtils.isNotEmpty(genreOrdinals)) {
                return StringUtils.join(",", genreOrdinals);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getLinks(JSONObject obj) {
        List<String> links = new ArrayList<>();
        JSONArray linksArray = JSONHelper.getArraySafe(obj, "links");
        if (linksArray != null) {
            for (Object object : linksArray) {
                if (object instanceof JSONObject) {
                    JSONObject linksObject = (JSONObject) object;
                    String link = JSONHelper.getStringSafe(linksObject, "link");
                    if (StringUtils.isNotEmpty(link)) {
                        links.add(link);
                    }
                }
            }
        }

        return StringUtils.join(",", links);
    }

    private static long getChaptersCount(JSONObject obj) {
        try {
            return JSONHelper.getLongSafe(obj, "chapters", -1);
        } catch (Exception ex) {
            return -1;
        }
    }

    private static List<BoundService> getBoundServices(JSONObject obj) {
        List<BoundService> boundServices = new ArrayList<>();
        JSONArray servicesArray = JSONHelper.getArraySafe(obj, "bound_services");
        if (servicesArray != null) {
            for (Object object : servicesArray) {
                if (object instanceof JSONObject) {
                    JSONObject servicesObject = (JSONObject) object;

                    ServiceType serviceType = EnumUtils.valueOfOrNull(ServiceType.class, JSONHelper.getStringSafe(servicesObject, "service_type"));
                    if (serviceType != null) {
                        String id = JSONHelper.getStringSafe(servicesObject, "id");
                        String link = JSONHelper.getStringSafe(servicesObject, "link");

                        if (StringUtils.isNotEmpty(id)) {
                            boundServices.add(new BoundService(serviceType, id, link));
                        }
                    }
                }
            }
        }

        return boundServices;
    }

    public static void putHashes(JSONObject obj, String serieHash, String archiveHash) {
        JSONObject atsumeru = new JSONObject();
        putJSON(atsumeru, "serie_hash", serieHash);
        putJSON(atsumeru, "hash", archiveHash);

        obj.put("atsumeru", atsumeru);
    }

    public static void putLinks(JSONObject obj, String linksStr) {
        List<String> links = ArrayUtils.splitString(linksStr, ",");
        if (ArrayUtils.isNotEmpty(links)) {
            JSONArray linksArray = new JSONArray();
            links.stream()
                    .filter(StringUtils::isNotEmpty)
                    .forEach(link -> {
                        JSONObject linksObj = new JSONObject();
                        putJSON(linksObj, "source", LinkUtils.getHostName(link));
                        putJSON(linksObj, "link", link);
                        linksArray.put(linksObj);
                    });

            obj.put("links", linksArray);
        }
    }

    public static void putBoundServices(JSONObject obj, List<BoundService> boundServices) {
        if (ArrayUtils.isNotEmpty(boundServices)) {
            JSONArray servicesArray = new JSONArray();

            boundServices.forEach(boundService -> {
                JSONObject serviceObj = new JSONObject();
                putJSON(serviceObj, "service_type", boundService.getServiceType());
                putJSON(serviceObj, "id", boundService.getId());
                putJSON(serviceObj, "link", boundService.getLink());
                servicesArray.put(serviceObj);
            });

            obj.put("bound_services", servicesArray);
        }
    }
}