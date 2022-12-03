package com.atsumeru.web.enums;

public enum Censorship {
    UNKNOWN(0),
    CENSORED(1),
    UNCENSORED(2),
    DECENSORED(3),
    PARTIALLY_CENSORED(4),
    MOSAIC_CENSORSHIP(5);

    public final int id;

    Censorship(int id) {
        this.id = id;
    }
}
