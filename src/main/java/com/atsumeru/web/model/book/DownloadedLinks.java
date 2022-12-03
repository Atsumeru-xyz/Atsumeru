package com.atsumeru.web.model.book;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class DownloadedLinks {
    @Expose
    private List<String> downloaded;

    @Expose
    @SerializedName("not_downloaded")
    private List<String> notDownloaded;
}
