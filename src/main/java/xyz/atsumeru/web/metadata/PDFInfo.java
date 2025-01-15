package xyz.atsumeru.web.metadata;

import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import xyz.atsumeru.web.model.book.BookArchive;

public class PDFInfo {

    public static boolean readInfo(BookArchive bookArchive, PDDocumentInformation info, Integer pagesCount) {
        bookArchive.setTitle(info.getTitle());
        bookArchive.setAuthors(info.getAuthor());
        bookArchive.setPublisher(info.getProducer());
        bookArchive.setTags(info.getKeywords());
        bookArchive.setPagesCount(pagesCount);
        return true;
    }
}
