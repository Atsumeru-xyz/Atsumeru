package xyz.atsumeru.web.model.service;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.atsumeru.web.model.covers.CoversCachingStatus;
import xyz.atsumeru.web.model.importer.ImportStatus;
import xyz.atsumeru.web.model.metadata.MetadataUpdateStatus;

@Data
@AllArgsConstructor
public class ServicesStatus {
    @Schema(name = "importer")
    @SerializedName("importer")
    private ImportStatus importStatus;

    @Schema(name = "metadata_update")
    @SerializedName("metadata_update")
    private MetadataUpdateStatus metadataUpdateStatus;

    @Schema(name = "covers_caching")
    @SerializedName("covers_caching")
    private CoversCachingStatus coversCachingStatus;
}
