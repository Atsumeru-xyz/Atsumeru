package xyz.atsumeru.web.model.book;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class DownloadedLinks {
    private List<String> downloaded;

    @Schema(name = "not_downloaded")
    @SerializedName("not_downloaded")
    private List<String> notDownloaded;
}
