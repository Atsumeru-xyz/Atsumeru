package com.atsumeru.web.model.book.franchise;

import com.atsumeru.web.model.book.service.BoundService;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Franchise {
    @Expose
    private int order;

    @Expose
    @SerializedName("content_type")
    private String contentType;

    @Expose
    private String title;

    @Expose
    @SerializedName("alt_title")
    private String altTitle;

    @Expose
    @SerializedName("jap_title")
    private String japTitle;

    @Expose
    private String year;

    @Expose
    // Base64 image
    private String cover;

    @Expose
    @SerializedName("bound_content")
    private List<BoundService> boundContent;
}
