package xyz.atsumeru.web.model.share;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.atsumeru.web.json.adapter.StringListBidirectionalAdapter;

@Data
public class CreateShareTokenRequest {
    @Schema(name = "name", description = "Display name of the share token", requiredMode = Schema.RequiredMode.REQUIRED)
    @SerializedName("name")
    private String name;

    @Schema(name = "serie_hash", description = "Hash of the book serie to share", requiredMode = Schema.RequiredMode.REQUIRED)
    @SerializedName("serie_hash")
    private String serieHash;

    @Schema(name = "authorities", description = "Additional authorities (e.g. DOWNLOAD_FILES)", implementation = String[].class)
    @JsonAdapter(StringListBidirectionalAdapter.class)
    private String authorities;

    @Schema(name = "expires_at", description = "Expiration timestamp in epoch millis, null for no expiration")
    @SerializedName("expires_at")
    private Long expiresAt;
}
