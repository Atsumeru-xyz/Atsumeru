package xyz.atsumeru.web.model.book.franchise;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import xyz.atsumeru.web.model.book.service.BoundService;

import java.util.List;

@Data
public class Franchise {
    private int order;

    @SerializedName("content_type")
    private String contentType;

    private String title;

    @SerializedName("alt_title")
    private String altTitle;

    @SerializedName("jap_title")
    private String japTitle;

    private String year;

    // Base64 image
    private String cover;

    @SerializedName("bound_content")
    private List<BoundService> boundContent;
}
