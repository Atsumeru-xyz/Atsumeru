package xyz.atsumeru.web.model.book.volume;

import com.google.gson.annotations.SerializedName;
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

    @SerializedName("additional_title")
    private String additionalTitle;

    private String year;

    @SerializedName("cover_accent")
    private String coverAccent;

    @SerializedName("file_name")
    private String fileName;

    @SerializedName("file_path")
    private String filePath;

    @Nullable
    @SerializedName("volume")
    private Float volume;

    @SerializedName("pages_count")
    private int pagesCount;

    @SerializedName("is_book")
    private boolean isBook;

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
