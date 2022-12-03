package com.atsumeru.web.model.book.service;

import com.atsumeru.web.util.GUString;
import com.atsumeru.web.enums.ServiceType;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

public class BoundService {
    @Getter
    @Expose
    @SerializedName("service_type")
    private final ServiceType serviceType;
    @Expose
    private final String id;
    @Getter
    @Expose
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
        return getRealId(GUString.getFirstNotEmptyValue(id, link));
    }

    private String getRealId(String idOrLink) {
        return GUString.startsWithIgnoreCase(idOrLink, "http")
                ? serviceType.extractId(idOrLink)
                : idOrLink;
    }
}
