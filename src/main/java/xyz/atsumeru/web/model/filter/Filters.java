package xyz.atsumeru.web.model.filter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Filters {
    private String id;
    private String name;
    @SerializedName("has_and_mode")
    private boolean hasAndMode;
    @SerializedName("single_mode")
    private boolean singleMode;
    private List<String> values;

    public static Filters create(String id, String name, boolean hasAndMode, boolean singleMode, List<String> values) {
        Filters filters = new Filters();
        filters.id = id;
        filters.name = name;
        filters.hasAndMode = hasAndMode;
        filters.singleMode = singleMode;
        filters.values = values;
        return filters;
    }
}
