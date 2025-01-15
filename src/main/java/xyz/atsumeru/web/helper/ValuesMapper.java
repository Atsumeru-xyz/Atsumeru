package xyz.atsumeru.web.helper;

import xyz.atsumeru.web.model.book.BaseBook;

import java.util.Optional;

public class ValuesMapper {

    public static String getMangaValue(BaseBook baseBook, String name, boolean lowerCaseArrays) {
        return switch (name) {
            case "title" -> baseBook.getTitle();
            case "alt_title", "alternative_title" -> baseBook.getAltTitle();
            case "jap_title" -> baseBook.getJapTitle();
            case "korean_title" -> baseBook.getKorTitle();
            case "link" -> baseBook.getContentLink();
            case "ihash" -> baseBook.getContentId();
            case "author", "authors" -> baseBook.getAuthors();
            case "summary" -> baseBook.getDescription();
            case "event", "events" -> baseBook.getEvent();
            case "publisher", "publishers" -> baseBook.getPublisher();
            case "production_year", "year", "years" -> baseBook.getYear();
            case "country", "countries" -> baseBook.getCountry();
            case "language", "languages" -> Optional.ofNullable(baseBook.getLanguage())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getLanguage());
            case "artists" -> Optional.ofNullable(baseBook.getArtists())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getArtists());
            case "translators" -> Optional.ofNullable(baseBook.getTranslators())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getTranslators());
            case "volume" -> String.valueOf(baseBook.getVolume());
            case "rating" -> String.valueOf(baseBook.getRating());
            case "score" -> baseBook.getScore();
            case "is_mature" -> String.valueOf(baseBook.getIsMature());
            case "is_adult" -> String.valueOf(baseBook.getIsAdult());
            case "censorship" -> String.valueOf(baseBook.getCensorship().toString());
            case "color" -> String.valueOf(baseBook.getColor().toString());
            case "status" -> baseBook.getStatus().name();
            case "translation_status" -> baseBook.getTranslationStatus().name();
            case "plot_type" -> baseBook.getPlotType().name();
            case "content_type" -> baseBook.getContentType().name();
            case "cover" -> baseBook.getCover();
            case "genres", "genre" -> Optional.ofNullable(baseBook.getGenres())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getGenres());
            case "tags" -> Optional.ofNullable(baseBook.getTags())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getTags());
            case "series" -> Optional.ofNullable(baseBook.getSeries())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getSeries());
            case "parodies" -> Optional.ofNullable(baseBook.getParodies())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getParodies());
            case "circles" -> Optional.ofNullable(baseBook.getCircles())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getCircles());
            case "magazines" -> Optional.ofNullable(baseBook.getMagazines())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getMagazines());
            case "characters" -> Optional.ofNullable(baseBook.getCharacters())
                    .map(value -> lowerCaseArrays ? value.toLowerCase() : value)
                    .orElse(baseBook.getCharacters());
            case "volumes", "volumes_count" -> String.valueOf(baseBook.getVolumesCount());
            case "chapters", "chapters_count" -> String.valueOf(baseBook.getChaptersCount());
            default -> null;
        };
    }
}