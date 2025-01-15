package xyz.atsumeru.web.model.service;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.atsumeru.web.model.covers.CoversCachingStatus;
import xyz.atsumeru.web.model.importer.ImportStatus;
import xyz.atsumeru.web.model.metadata.MetadataUpdateStatus;

@Data
@AllArgsConstructor
public class ServicesStatus {
    @SerializedName("importer")
    private ImportStatus importStatus;

    @SerializedName("metadata_update")
    private MetadataUpdateStatus metadataUpdateStatus;

    @SerializedName("covers_caching")
    private CoversCachingStatus coversCachingStatus;
}
