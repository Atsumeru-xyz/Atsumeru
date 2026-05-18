package xyz.atsumeru.web.model.book.service;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import xyz.atsumeru.web.enums.ServiceType;
import xyz.atsumeru.web.util.StringUtils;

public class BoundService {
    @Getter
    @Schema(name = "service_type")
    @SerializedName("service_type")
    private ServiceType serviceType;

    private String id;

    @Getter
    private String link;

    public BoundService(ServiceType serviceType, String idOrLink) {
        this.serviceType = serviceType;
        this.id = getRealId(idOrLink);
        this.link = serviceType.createUrl(id);
    }

    public BoundService(ServiceType serviceType, String id, String link) {
        this.serviceType = serviceType;
        this.id = id;
        this.link = link;
    }

    public String getId() {
        return getRealId(StringUtils.getFirstNotEmptyValue(id, link));
    }

    private String getRealId(String idOrLink) {
        return StringUtils.startsWithIgnoreCase(idOrLink, "http")
                ? serviceType.extractId(idOrLink)
                : idOrLink;
    }
}
