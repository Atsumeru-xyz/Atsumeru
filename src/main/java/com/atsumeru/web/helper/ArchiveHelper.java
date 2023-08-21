package com.atsumeru.web.helper;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.util.*;
import com.atsumeru.web.archive.ArchiveReader;
import com.atsumeru.web.archive.iterator.IArchiveIterator;
import com.atsumeru.web.enums.BookType;
import com.atsumeru.web.exception.ArchiveReadingException;
import com.atsumeru.web.exception.NoReadableFoundException;
import com.atsumeru.web.exception.PageNotFoundException;
import com.atsumeru.web.renderer.RendererFactory;
import lombok.SneakyThrows;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;

public class ArchiveHelper {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveHelper.class.getSimpleName());

    public static void saveBookPage(File outputFile, String archiveHash, String chapterHash, int page) {
        try (OutputStream out = new FileOutputStream(outputFile)) {
            getBookPage(null, out, archiveHash, chapterHash, page, false, 1);
        } catch (Exception ex) {
            outputFile.delete();
        }
    }

    public static void getBookPage(HttpServletResponse response, String archiveHash, int page, boolean reDecodeImage) {
        getBookPage(response, archiveHash, null, page, reDecodeImage);
    }

    @SneakyThrows
    public static void getBookPage(HttpServletResponse response, String archiveHash, String chapterHash, int page, boolean convertImage) {
        getBookPage(response, response.getOutputStream(), archiveHash, chapterHash, page, convertImage, 1);
    }

    private static void getBookPage(@Nullable HttpServletResponse response, OutputStream outputStream, @Nullable String archiveHash,
                                    @Nullable String chapterHash, int page, boolean convertImage, int tryCount) throws InterruptedException {
        if (StringUtils.isEmpty(archiveHash) && StringUtils.isNotEmpty(chapterHash) || BooksRepository.isArchiveHash(archiveHash)) {
            long time = System.currentTimeMillis();

            // Поиск архива по archive_hash или chapter_hash
            IBaseBookItem archiveItem = findArchive(archiveHash, chapterHash);

            // Проверка не книга ли это и если так, то применить иные методы чтения
            if (archiveItem instanceof BookArchive) {
                BookArchive archive = (BookArchive) archiveItem;
                if (archive.isBook()) {
                    BookType bookType = ContentDetector.detectBookType(Paths.get(archive.getFolder()));
                    RendererFactory.create(bookType, archive.getFolder()).renderPage(response, outputStream, page);
                    return;
                }
            }

            // Открытие архива для чтения
            try (IArchiveIterator archiveIterator = ArchiveReader.getArchiveIterator(archiveItem.getFolder())) {
                List<String> pages = StringUtils.isNotEmpty(chapterHash)
                        ? BooksRepository.getChapter(chapterHash).getPageEntryNames()
                        : archiveItem.getPageEntryNames();

                if (!writePageIntoResponse(response, outputStream, archiveIterator, pages, page, convertImage, time)) {
                    throw new PageNotFoundException();
                }
                return;
            } catch (Exception ex) {
                if ((ex.getMessage().equals("Stream closed") || ex instanceof ClientAbortException) && tryCount < 5) {
                    Thread.sleep(1000);
                    getBookPage(response, outputStream, archiveHash, chapterHash, page, convertImage, ++tryCount);
                    return;
                } else {
                    ex.printStackTrace();
                    throw new ArchiveReadingException();
                }
            }
        }

        throw new NoReadableFoundException();
    }

    private static IBaseBookItem findArchive(@Nullable String archiveHash, @Nullable String chapterHash) {
        return BooksRepository.getBookDetails(
                NotEmptyString.ofNullable(archiveHash)
                        .orElseGet(() -> BooksRepository.getChapter(chapterHash).getArchiveId()));
    }

    private static boolean writePageIntoResponse(@Nullable HttpServletResponse response, OutputStream outputStream, IArchiveIterator archiveIterator,
                                                 List<String> pages, int page, boolean convertImage, long time) throws IOException {
        if (ArrayUtils.isNotEmpty(pages) && pages.size() >= page) {
            return writeEntryStreamIntoResponseOrOutputStream(
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

    public static boolean writeEntryStreamIntoResponseOrOutputStream(@Nullable HttpServletResponse response, OutputStream outputStream, InputStream entryInputStream,
                                                                     long contentLength, String pagePath, boolean convertImage, long timeStart) {
        TikaInputStream tikaInputStream = null;
        try {
            setResponseContentTypeAndLength(response, FilesHelper.safeProbeContentType(pagePath), (int) contentLength);

            if (convertImage) {
                tikaInputStream = ContentDetector.createTikaInputStream(entryInputStream);
                if (ContentDetector.isWebP(tikaInputStream)) {
                    BufferedImage bufferedImage = ImageIO.read(tikaInputStream);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "jpeg", os);
                    setResponseContentTypeAndLength(response, "image/jpeg", os.size());
                    IOUtils.copy(entryInputStream = new ByteArrayInputStream(os.toByteArray()), outputStream);
                } else {
                    IOUtils.copy(tikaInputStream, outputStream);
                }
            } else {
                IOUtils.copy(entryInputStream, outputStream);
            }

            if (response != null) {
                logger.info("Image unpacking and writing time: " + (System.currentTimeMillis() - timeStart) + "ms. Image length: " + contentLength + " bytes");
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            FileUtils.closeQuietly(tikaInputStream);
            FileUtils.closeQuietly(entryInputStream);
        }

        return false;
    }

    private static void setResponseContentTypeAndLength(@Nullable HttpServletResponse response, String mimeType, int contentLength) {
        if (response != null) {
            response.setContentType(mimeType);
            response.setContentLength(contentLength);
        }
    }
}
