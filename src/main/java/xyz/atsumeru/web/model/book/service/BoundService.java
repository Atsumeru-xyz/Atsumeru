package xyz.atsumeru.web.model.book.service;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import xyz.atsumeru.web.enums.ServiceType;
import xyz.atsumeru.web.util.StringUtils;

public class BoundService {
    @Getter
    @SerializedName("service_type")
    private final ServiceType serviceType;

    private final String id;

    @Getter
    private final String link;

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
