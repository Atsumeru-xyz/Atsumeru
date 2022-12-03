package com.atsumeru.web.metadata;

import com.atsumeru.web.model.book.BookArchive;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class PDFInfo {

    public static boolean readInfo(BookArchive bookArchive, PDDocumentInformation info, Integer pagesCount) {
        bookArchive.setTitle(info.getTitle());
        bookArchive.setAuthors(info.getAuthor());
        bookArchive.setProducers(info.getProducer());
        bookArchive.setTags(info.getKeywords());
        bookArchive.setPagesCount(pagesCount);
        return true;
    }
}
