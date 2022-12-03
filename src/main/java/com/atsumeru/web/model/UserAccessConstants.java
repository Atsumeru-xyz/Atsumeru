package com.atsumeru.web.model;

import com.atsumeru.web.model.database.Category;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserAccessConstants {
    @Expose
    private List<String> roles;

    @Expose
    private List<String> authorities;

    @Expose
    private List<Category> categories;

    @Expose
    private List<GenreModel> genres;

    @Expose
    private List<String> tags;
}
