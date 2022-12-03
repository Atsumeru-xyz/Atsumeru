package com.atsumeru.web.model;

import com.atsumeru.web.enums.Genre;
import com.atsumeru.web.component.Localizr;
import com.google.gson.annotations.Expose;

public class GenreModel {
    @Expose
    private String name;

    @Expose
    private int id;

    public GenreModel(Genre genre) {
        this.name = Localizr.toLocale("genre_" + genre.toString().toLowerCase());
        this.id = genre.ordinal();
    }
}
