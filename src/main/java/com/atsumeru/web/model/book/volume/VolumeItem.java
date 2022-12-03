package com.atsumeru.web.model.book.volume;

import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.database.History;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

@Data
public class VolumeItem implements Serializable {
    @Expose
    private String id;

    @Expose
    private String title;

    @Expose
    @SerializedName("additional_title")
    private String additionalTitle;

    @Expose
    private String year;

    @Expose
    @SerializedName("cover_accent")
    private String coverAccent;

    @Expose
    @SerializedName("file_name")
    private String fileName;

    @Expose
    @SerializedName("file_path")
    private String filePath;

    @Expose
    @Nullable
    @SerializedName("volume")
    private Float volume;

    @Expose
    @SerializedName("pages_count")
    private int pagesCount;

    @Expose
    @SerializedName("is_book")
    private boolean isBook;

    @Expose
    @SerializedName("created_at")
    private long createdAt;

    @Expose
    private History history;

    @Expose
    private Collection<BookChapter> chapters;

    public boolean isRead() {
        return Optional.ofNullable(history)
                .map(model -> model.getCurrentPage().equals(model.getPagesCount()))
                .orElse(false);
    }
}
