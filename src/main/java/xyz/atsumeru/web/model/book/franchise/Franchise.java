package xyz.atsumeru.web.model.book.franchise;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.atsumeru.web.model.book.service.BoundService;

import java.util.List;

@Data
public class Franchise {
    private int order;

    @Schema(name = "content_type")
    @SerializedName("content_type")
    private String contentType;

    private String title;

    @Schema(name = "alt_title")
    @SerializedName("alt_title")
    private String altTitle;

    @Schema(name = "jap_title")
    @SerializedName("jap_title")
    private String japTitle;

    private String year;

    // Base64 image
    private String cover;

    @Schema(name = "bound_content")
    @SerializedName("bound_content")
    private List<BoundService> boundContent;
}
