package com.atsumeru.web.enums;

import com.atsumeru.web.model.database.DatabaseFields;
import com.atsumeru.web.util.StringUtils;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ServiceType {
    MYANIMELIST("mal", "https://myanimelist.net/manga/%s", Pattern.compile("manga/(\\d+)"), DatabaseFields.MAL_ID),
    SHIKIMORI("shiki", "https://shikimori.me/mangas/%s", Pattern.compile("/(\\d+)|/\\w(\\d+)"), DatabaseFields.SHIKIMORI_ID),
    KITSU("kt", "https://kitsu.io/manga/%s", Pattern.compile("manga/(.*)/|manga/(.*)"), DatabaseFields.KITSU_ID),
    ANILIST("al", "https://anilist.co/manga/%s", Pattern.compile("manga/(\\d+)"), DatabaseFields.ANILIST_ID),
    MANGAUPDATES("mu", "https://www.mangaupdates.com/series/%s", Pattern.compile("series/(.*?)/|series/(.*?)$"), DatabaseFields.MANGAUPDATES_ID),
    ANIMEPLANET("ap", "https://www.anime-planet.com/manga/%s", Pattern.compile("manga/(.*)/|manga/(.*)"), DatabaseFields.ANIMEPLANET_ID),
    COMICVINE("cv", "https://comicvine.gamespot.com/comic/%s/", Pattern.compile("(\\d+-\\d+)"), DatabaseFields.COMICVINE_ID),
    COMICSDB("cdb", "https://comicsdb.ru/publishers/%s", Pattern.compile("publishers/(.*)"), DatabaseFields.COMICSDB_ID),
    HENTAG("htg", "https://hentag.com/vault/%s", Pattern.compile("vault/(.*)"), DatabaseFields.HENTAG_ID);

    @Getter
    private final String simpleName;
    private final String formatUrl;
    private final Pattern idPattern;
    @Getter
    private final String dbFieldName;

    ServiceType(String simpleName, String formatUrl, Pattern idPattern, String dbFieldName) {
        this.simpleName = simpleName;
        this.formatUrl = formatUrl;
        this.idPattern = idPattern;
        this.dbFieldName = dbFieldName;
    }

    public String extractId(String str) {
        Matcher matcher = idPattern.matcher(str);
        if (matcher.find()) {
            String firstGroup = matcher.group(1);
            String secondGroup = null;
            try {
                secondGroup = matcher.group(2);
            } catch (Exception ignored) {
            }
            return StringUtils.getFirstNotEmptyValue(firstGroup, secondGroup);
        }
        return null;
    }

    public String createUrl(String id) {
        return String.format(formatUrl, id);
    }

    public static @Nullable ServiceType getTypeBySimpleName(@Nullable String name) {
        return Arrays.stream(ServiceType.values())
                .filter(serviceType -> StringUtils.equalsIgnoreCase(serviceType.name(), name) || StringUtils.equalsIgnoreCase(serviceType.getSimpleName(), name))
                .findFirst()
                .orElse(null);
    }

    public static @Nullable String getDbFieldNameForSimpleName(@Nullable String name) {
        return Optional.ofNullable(getTypeBySimpleName(name))
                .map(ServiceType::getDbFieldName)
                .orElse(null);
    }
}
