package com.atsumeru.web.metadata;

import com.atsumeru.web.model.book.BookArchive;

public class DjVuInfo {

    public static void readInfo(BookArchive bookArchive, Integer pagesCount) {
        bookArchive.setTitle(bookArchive.getTitle());
        bookArchive.setPagesCount(pagesCount);
    }
}
