package com.atsumeru.web.util;

import com.atsumeru.web.model.book.BaseBook;

import java.util.Optional;

public class ValuesMapper {

    public static String getMangaValue(BaseBook baseBook, String name, boolean lowerCaseArrays) {
        switch (name) {
            case "title":
                return baseBook.getTitle();
            case "alt_title":
            case "alternative_title":
                return baseBook.getAltTitle();
            case "jap_title":
                return baseBook.getJapTitle();
            case "korean_title":
                return baseBook.getKorTitle();
            case "link":
                return baseBook.getContentLink();
            case "ihash":
                return baseBook.getContentId();
            case "author":
            case "authors":
                return baseBook.getAuthors();
            case "summary":
                return baseBook.getDescription();
            case "event":
            case "events":
                return baseBook.getEvent();
            case "publisher":
            case "publishers":
                return baseBook.getPublisher();
            case "production_year":
            case "year":
            case "years":
                return baseBook.getYear();
            case "country":
            case "countries":
                return baseBook.getCountry();
            case "language":
            case "languages":
                return Optional.ofNullable(baseBook.getLanguage())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getLanguage());
            case "artists":
                return Optional.ofNullable(baseBook.getArtists())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getArtists());
            case "translators":
                return Optional.ofNullable(baseBook.getTranslators())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getTranslators());
            case "volume":
                return String.valueOf(baseBook.getVolume());
            case "rating":
                return String.valueOf(baseBook.getRating());
            case "score":
                return baseBook.getScore();
            case "is_mature":
                return String.valueOf(baseBook.getIsMature());
            case "is_adult":
                return String.valueOf(baseBook.getIsAdult());
            case "censorship":
                return String.valueOf(baseBook.getCensorship().toString());
            case "color":
                return String.valueOf(baseBook.getColor().toString());
            case "status":
                return baseBook.getStatus().name();
            case "translation_status":
                return baseBook.getTranslationStatus().name();
            case "plot_type":
                return baseBook.getPlotType().name();
            case "content_type":
                return baseBook.getContentType().name();
            case "cover":
                return baseBook.getCover();
            case "genres":
            case "genre":
                return Optional.ofNullable(baseBook.getGenres())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getGenres());
            case "tags":
                return Optional.ofNullable(baseBook.getTags())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getTags());
            case "series":
                return Optional.ofNullable(baseBook.getSeries())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getSeries());
            case "parodies":
                return Optional.ofNullable(baseBook.getParodies())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getParodies());
            case "circles":
                return Optional.ofNullable(baseBook.getCircles())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getCircles());
            case "magazines":
                return Optional.ofNullable(baseBook.getMagazines())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getMagazines());
            case "characters":
                return Optional.ofNullable(baseBook.getCharacters())
                        .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                        .orElse(baseBook.getCharacters());
            case "volumes":
            case "volumes_count":
                return String.valueOf(baseBook.getVolumesCount());
            case "chapters":
            case "chapters_count":
                return String.valueOf(baseBook.getChaptersCount());
            default:
                return null;
        }
    }
}