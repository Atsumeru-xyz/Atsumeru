package com.atsumeru.web.model.filter;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Filters {
    @Expose
    private String id;

    @Expose
    private String name;

    @Expose
    @SerializedName("has_and_mode")
    private boolean hasAndMode;

    @Expose
    @SerializedName("single_mode")
    private boolean singleMode;

    @Expose
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
