package xyz.atsumeru.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.atsumeru.web.model.database.Category;

import java.util.List;

@Data
@AllArgsConstructor
public class UserAccessConstants {
    private List<String> roles;
    private List<String> authorities;
    private List<Category> categories;
    private List<GenreModel> genres;
    private List<String> tags;
}
