package xyz.atsumeru.web.model.book;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class DownloadedLinks {
    private List<String> downloaded;

    @SerializedName("not_downloaded")
    private List<String> notDownloaded;
}
