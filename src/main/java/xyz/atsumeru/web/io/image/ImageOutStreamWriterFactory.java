package xyz.atsumeru.web.io.image;

import org.springframework.lang.Nullable;
import xyz.atsumeru.web.exception.NoReadableFoundException;
import xyz.atsumeru.web.io.image.impl.ArchivedImageOutStreamWriter;
import xyz.atsumeru.web.io.image.impl.RenderedImageOutStreamWriter;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.util.StringUtils;

import java.util.Optional;

public class ImageOutStreamWriterFactory {

    public static ImageOutStreamWriter create(String archiveHash, @Nullable String chapterHash) {
        if (StringUtils.isEmpty(archiveHash) && StringUtils.isNotEmpty(chapterHash) || BooksRepository.isArchiveHash(archiveHash)) {
            IBaseBookItem baseBookItem = findArchive(archiveHash, chapterHash);

            return baseBookItem instanceof BookArchive archive && archive.isBook()
                    ? new RenderedImageOutStreamWriter(baseBookItem)
                    : new ArchivedImageOutStreamWriter(baseBookItem, chapterHash);
        }
        throw new NoReadableFoundException();
    }

    private static IBaseBookItem findArchive(@Nullable String archiveHash, @Nullable String chapterHash) {
        return BooksRepository.getBookDetails(
                Optional.ofNullable(archiveHash)
                        .filter(StringUtils::isNotEmpty)
                        .orElseGet(() -> BooksRepository.getChapter(chapterHash).getArchiveId()));
    }
}
