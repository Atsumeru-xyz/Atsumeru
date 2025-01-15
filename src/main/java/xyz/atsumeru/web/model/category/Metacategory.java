package xyz.atsumeru.web.model.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Metacategory {
    private String id;
    private String name;
    private long count;
}
