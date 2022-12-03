package com.atsumeru.web.enums;

public enum TranslationStatus {
    UNKNOWN(0),
    ONGOING(1),
    COMPLETE(2),
    ON_HOLD(3),
    DROPPED(4);

    public final int id;

    TranslationStatus(int id) {
        this.id = id;
    }
}
