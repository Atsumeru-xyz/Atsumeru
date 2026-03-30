package xyz.atsumeru.web.model.book.volume;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.lang.Nullable;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.database.History;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

@Data
public class VolumeItem implements Serializable {
    private String id;
    private String title;

    @Schema(name = "additional_title")
    @SerializedName("additional_title")
    private String additionalTitle;

    private String year;

    @Schema(name = "cover_accent")
    @SerializedName("cover_accent")
    private String coverAccent;

    @Schema(name = "file_name")
    @SerializedName("file_name")
    private String fileName;

    @Schema(name = "file_path")
    @SerializedName("file_path")
    private String filePath;

    @Nullable
    @Schema(name = "volume")
    @SerializedName("volume")
    private Float volume;

    @Schema(name = "pages_count")
    @SerializedName("pages_count")
    private int pagesCount;

    @Schema(name = "is_book")
    @SerializedName("is_book")
    private boolean isBook;

    @Schema(name = "created_at")
    @SerializedName("created_at")
    private long createdAt;

    private History history;

    private Collection<BookChapter> chapters;

    public boolean isRead() {
        return Optional.ofNullable(history)
                .map(model -> model.getCurrentPage().equals(model.getPagesCount()))
                .orElse(false);
    }
}
