package xyz.atsumeru.web.model.database;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.atsumeru.web.json.adapter.StringListBidirectionalAdapter;
import xyz.atsumeru.web.util.ArrayUtils;

import java.util.HashSet;
import java.util.Set;

@Data
@DatabaseTable(tableName = "SHARE_TOKENS")
public class ShareToken {
    @Expose(serialize = false)
    @DatabaseField(generatedId = true)
    private Long id;

    @Schema(name = "token")
    @SerializedName("token")
    @DatabaseField(columnName = "TOKEN", unique = true)
    private String token;

    @Schema(name = "name", description = "Display name of the share token")
    @SerializedName("name")
    @DatabaseField(columnName = "NAME")
    private String name;

    @Schema(name = "serie_hash")
    @SerializedName("serie_hash")
    @DatabaseField(columnName = "SERIE_HASH")
    private String serieHash;

    @Schema(name = "serie_name", description = "Title of the shared book serie", accessMode = Schema.AccessMode.READ_ONLY)
    @SerializedName("serie_name")
    @DatabaseField(columnName = "SERIE_NAME")
    private String serieName;

    @JsonAdapter(StringListBidirectionalAdapter.class)
    @DatabaseField(columnName = "AUTHORITIES")
    private String authorities;

    @Schema(name = "created_by")
    @SerializedName("created_by")
    @DatabaseField(columnName = "CREATED_BY")
    private String createdBy;

    @Schema(name = "created_at")
    @SerializedName("created_at")
    @DatabaseField(columnName = "CREATED_AT")
    private Long createdAt;

    @Schema(name = "expires_at")
    @SerializedName("expires_at")
    @DatabaseField(columnName = "EXPIRES_AT")
    private Long expiresAt;

    @Schema(hidden = true)
    public Set<String> getAuthoritiesSet() {
        Set<String> set = new HashSet<>(ArrayUtils.splitString(getAuthorities(), ","));
        set.add("ROLE_SHARE");
        return set;
    }
}
