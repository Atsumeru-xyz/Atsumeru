package com.atsumeru.web.model.category;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Metacategory {
    @Expose private String id;
    @Expose private String name;
    @Expose private long count;
}
