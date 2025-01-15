package xyz.atsumeru.web.io.image.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import xyz.atsumeru.web.archive.ArchiveReader;
import xyz.atsumeru.web.archive.iterator.IArchiveIterator;
import xyz.atsumeru.web.exception.ArchiveReadingException;
import xyz.atsumeru.web.exception.PageNotFoundException;
import xyz.atsumeru.web.helper.FilesHelper;
import xyz.atsumeru.web.io.image.ImageOutStreamWriter;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.util.AppUtils;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ArchivedImageOutStreamWriter implements ImageOutStreamWriter {
    private static final Logger logger = LoggerFactory.getLogger(ArchivedImageOutStreamWriter.class.getSimpleName());

    private final IBaseBookItem baseBookItem;
    private final String chapterHash;

    public ArchivedImageOutStreamWriter(IBaseBookItem baseBookItem, String chapterHash) {
        this.baseBookItem = baseBookItem;
        this.chapterHash = chapterHash;
    }

    @Override
    public void write(HttpServletResponse response, OutputStream responseOut, int page, boolean convertImage) {
        tryWrite(response, responseOut, page, convertImage, 1);
    }

    private void tryWrite(HttpServletResponse response, OutputStream responseOut, int page, boolean convertImage, int tryCount) {
        long time = System.currentTimeMillis();

        try (IArchiveIterator archiveIterator = ArchiveReader.getArchiveIterator(baseBookItem.getFolder())) {
            List<String> pages = StringUtils.isNotEmpty(chapterHash)
                    ? BooksRepository.getChapter(chapterHash).getPageEntryNames()
                    : baseBookItem.getPageEntryNames();

            if (!writePageIntoResponse(response, responseOut, archiveIterator, pages, page, convertImage, time)) {
                throw new PageNotFoundException();
            }
        } catch (Exception ex) {
            if ((ex.getMessage().equals("Stream closed") || ex instanceof ClientAbortException) && tryCount < 5) {
                AppUtils.sleepThread(1000);
                tryWrite(response, responseOut, page, convertImage, ++tryCount);
            } else {
                ex.printStackTrace();
                throw new ArchiveReadingException();
            }
        }
    }

    private boolean writePageIntoResponse(@Nullable HttpServletResponse response, OutputStream outputStream, IArchiveIterator archiveIterator,
                                          List<String> pages, int page, boolean convertImage, long time) throws IOException {
        if (ArrayUtils.isNotEmpty(pages) && pages.size() >= page) {
            return FilesHelper.writeEntryStreamIntoResponseOrOutputStream(
                    logger,
                    response,
                    outputStream,
                    archiveIterator.getEntryInputStreamByName(pages.get(page - 1)),
                    archiveIterator.getEntrySize(),
                    archiveIterator.getEntryName(),
                    convertImage,
                    time
            );
        }
        return false;
    }
}
