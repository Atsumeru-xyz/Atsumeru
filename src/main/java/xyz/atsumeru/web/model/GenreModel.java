package xyz.atsumeru.web.model;

import xyz.atsumeru.web.component.Localization;
import xyz.atsumeru.web.enums.Genre;

public class GenreModel {
    private final String name;
    private final int id;

    public GenreModel(Genre genre) {
        this.name = Localization.toLocale("genre_" + genre.toString().toLowerCase());
        this.id = genre.ordinal();
    }
}
