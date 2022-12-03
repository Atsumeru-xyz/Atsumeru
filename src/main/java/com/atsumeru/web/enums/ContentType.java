package com.atsumeru.web.enums;

import java.util.Arrays;
import java.util.List;

public enum ContentType {
    UNKNOWN(0),

    // Text (images)
    MANGA(10),
    MANHUA(11),
    MANHWA(12),
    DOUJINSHI(13),
    HENTAI_MANGA(14),
    YAOI(15),
    YAOI_MANGA(16),
    WEBCOMICS(17),
    RUMANGA(18),
    OEL_MANGA(19),
    STRIP(20),
    COMICS(21),
    YURI(26),
    YURI_MANGA(27),
    HENTAI_MANHWA(28),

    // Text (books)
    LIGHT_NOVEL(22),
    NOVEL(23),
    BOOK(24),
    TEXT_PORN(25),

    // Video
    ANIME(30),
    HENTAI(31),
    HENTAI_ANIME(32),
    YAOI_ANIME(33),
    DORAMA(34),
    CARTOON(35),
    MOVIES_TV(36),
    MOVIE(37),
    TV(38),
    PORN(39),
    YURI_ANIME(40),

    // Audio
    PODCAST(50),
    AUDIO(51),
    AUDIO_MUSIC(52),
    AUDIO_BOOK(53),
    AUDIO_NOVEL(54),
    AUDIO_LIGHT_NOVEL(55),
    AUDIO_PORN(56);

    public final int id;

    ContentType(int id) {
        this.id = id;
    }

    public static List<ContentType> getSupportedTypes() {
        return Arrays.asList(UNKNOWN, MANGA, MANHWA, MANHUA, COMICS, WEBCOMICS, LIGHT_NOVEL, HENTAI_MANGA, HENTAI_MANHWA,
                DOUJINSHI, YURI_MANGA, YAOI_MANGA, NOVEL, BOOK, RUMANGA, OEL_MANGA, STRIP);
    }

    public static boolean isMatureContent(ContentType contentType) {
        return contentType == HENTAI
                || contentType == HENTAI_ANIME
                || contentType == HENTAI_MANGA
                || contentType == HENTAI_MANHWA
                || contentType == YAOI
                || contentType == YAOI_ANIME
                || contentType == YAOI_MANGA
                || contentType == AUDIO_PORN
                || contentType == TEXT_PORN;
    }
}

