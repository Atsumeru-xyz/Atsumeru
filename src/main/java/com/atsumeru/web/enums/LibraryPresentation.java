package com.atsumeru.web.enums;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;

public enum LibraryPresentation {
    SERIES,
    SINGLES,
    ARCHIVES,
    SERIES_AND_SINGLES;

    public Class<? extends IBaseBookItem> getDbClassForPresentation() {
        switch (this) {
            case SERIES:
            case SINGLES:
            case SERIES_AND_SINGLES:
                return BookSerie.class;
            case ARCHIVES:
                return BookArchive.class;
        }
        throw new RuntimeException("Unknown class for presentation");
    }

    public boolean isSeriesOrSinglesPresentation() {
        return this == SERIES || this == SINGLES || this == SERIES_AND_SINGLES;
    }

    public boolean isSeriesAndSinglesPresentation() {
        return this == SERIES_AND_SINGLES;
    }

    public boolean isSeriesPresentation() {
        return this == SERIES;
    }

    public boolean isSinglesPresentation() {
        return this == SINGLES;
    }

    public boolean isArchivesPresentation() {
        return this == ARCHIVES;
    }
}
