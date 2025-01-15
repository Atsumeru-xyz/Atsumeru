package xyz.atsumeru.web.enums;

import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.IBaseBookItem;

public enum LibraryPresentation {
    SERIES,
    SINGLES,
    ARCHIVES,
    SERIES_AND_SINGLES;

    public Class<? extends IBaseBookItem> getDbClassForPresentation() {
        return switch (this) {
            case SERIES, SINGLES, SERIES_AND_SINGLES -> BookSerie.class;
            case ARCHIVES -> BookArchive.class;
        };
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
