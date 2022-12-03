package com.atsumeru.web.model.service;

import com.atsumeru.web.model.covers.CoversCachingStatus;
import com.atsumeru.web.model.importer.ImportStatus;
import com.atsumeru.web.model.metadata.MetadataUpdateStatus;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServicesStatus {
    @Expose
    @SerializedName("importer")
    private ImportStatus importStatus;

    @Expose
    @SerializedName("metadata_update")
    private MetadataUpdateStatus metadataUpdateStatus;

    @Expose
    @SerializedName("covers_caching")
    private CoversCachingStatus coversCachingStatus;
}
