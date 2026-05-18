package xyz.atsumeru.web.helper;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.BookType;
import xyz.atsumeru.web.exception.DownloadsNotAllowedException;
import xyz.atsumeru.web.exception.NoReadableFoundException;
import xyz.atsumeru.web.exception.RendererNotImplementedException;
import xyz.atsumeru.web.io.image.ImageOutStreamWriterFactory;
import xyz.atsumeru.web.manager.ImageCache;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.book.image.Images;
import xyz.atsumeru.web.model.importer.ReadableContent;
import xyz.atsumeru.web.renderer.RendererFactory;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.security.service.UsersDetailsService;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.ContentDetector;
import xyz.atsumeru.web.util.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FilesHelper {
    private static final Logger logger = LoggerFactory.getLogger(FilesHelper.class.getSimpleName());
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(Constants.Formats.JPG, Constants.MimeTypes.IMAGE_JPG);
        MIME_TYPES.put(Constants.Formats.JPEG, Constants.MimeTypes.IMAGE_JPEG);
        MIME_TYPES.put(Constants.Formats.PNG, Constants.MimeTypes.IMAGE_PNG);
        MIME_TYPES.put(Constants.Formats.GIF, Constants.MimeTypes.IMAGE_GIF);
        MIME_TYPES.put(Constants.Formats.WEBP, Constants.MimeTypes.IMAGE_WEBP);
        MIME_TYPES.put(Constants.Formats.AVIF, Constants.MimeTypes.IMAGE_AVIF);
        MIME_TYPES.put(Constants.Formats.HEIC, Constants.MimeTypes.IMAGE_HEIC);
        MIME_TYPES.put(Constants.Formats.HEIF, Constants.MimeTypes.IMAGE_HEIF);
        MIME_TYPES.put(Constants.Formats.JXL, Constants.MimeTypes.IMAGE_JXL);
    }

    public static String readHashFileAttribute(Path path, String attributeName, String defaultHash) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
                Object attr = Files.getAttribute(path, attributeName, LinkOption.NOFOLLOW_LINKS);
                if (attr instanceof byte[] attribute) {
                    return new String(attribute);
                }
            }
        } catch (Exception ignored) {
        }
        return defaultHash;
    }

    public static void writeHashFileAttribute(Path path, String attributeName, String hash) {
        try {
            Files.setAttribute(path, attributeName, hash.getBytes(StandardCharsets.UTF_8), LinkOption.NOFOLLOW_LINKS);
        } catch (Exception ignored) {
        }
    }

    public static void setAttributeHidden(File file) {
        try {
            Files.setAttribute(file.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ignored) {
        }
    }

    public static byte[] getCover(HttpServletResponse response, String imageHash, ImageCache.ImageCacheType cacheType, boolean convertImage) {
        if (cacheType != ImageCache.ImageCacheType.ORIGINAL) {
            return ImageCache.getImageBytesFromCache(imageHash, cacheType);
        } else {
            getOriginalImageFromArchive(response, imageHash, convertImage);
        }
        return null;
    }

    private static void getOriginalImageFromArchive(HttpServletResponse response, String imageHash, boolean convertImage) {
        if (BooksRepository.isSeriesHash(imageHash)) {
            List<IBaseBookItem> archivesList = BooksRepository.getArchivesForSerie(imageHash);
            if (ArrayUtils.isNotEmpty(archivesList)) {
                IBaseBookItem book = archivesList.get(0);

                File externalCover = ReadableContent.getSerieExternalCover(book.getFolder());
                if (externalCover != null) {
                    try {
                        writeEntryStreamIntoResponseOrOutputStream(
                                logger,
                                response,
                                response.getOutputStream(),
                                Files.newInputStream(externalCover.toPath()),
                                externalCover.length(),
                                externalCover.getAbsolutePath(),
                                convertImage,
                                System.currentTimeMillis()
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                } else {
                    imageHash = archivesList.get(0).getCover();
                }
            }
        }

        try {
            ImageOutStreamWriterFactory.create(imageHash, null).write(response, response.getOutputStream(), 1, convertImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean saveBookImageIntoCache(String imageHash) {
        IBaseBookItem book = BooksRepository.getBookDetails(imageHash);

        String itemHash = imageHash;
        boolean asSingle = book.isSingle();

        if (BooksRepository.isSeriesHash(itemHash)) {
            asSingle = BooksRepository.getBookDetails(itemHash).isSingle();
            List<IBaseBookItem> archivesList = BooksRepository.getArchivesForSerie(imageHash);
            if (ArrayUtils.isNotEmpty(archivesList)) {
                itemHash = archivesList.get(0).getCover();
            }
        }

        List<BookArchive> archives = Beans.getBooksDaoManager().query(itemHash, BookArchive.class);
        if (ArrayUtils.isNotEmpty(archives)) {
            Images images = ReadableContent.saveCoverImage(archives.get(0), asSingle);
            archives.forEach(baseBook -> {
                baseBook.setCoverAccent(Optional.ofNullable(images)
                        .map(Images::getAccent)
                        .orElse(baseBook.getCoverAccent()));
                Beans.getBooksDaoManager().save(baseBook);
            });
        }

        return FileUtils.isFileExist(ImageCache.getImage(imageHash, Constants.Formats.PNG, ImageCache.ImageCacheType.THUMBNAIL));
    }

    private static void writeResponseStream(HttpServletResponse response, File file) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(file)) {
            response.setContentType(Files.probeContentType(file.toPath()));
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(FileUtils.getFileNameWithExt(file.getPath(), true), StandardCharsets.UTF_8));
            IOUtils.copy(fis, response.getOutputStream());
        } catch (IOException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }
    }

    public static void downloadFile(HttpServletResponse response, Authentication authentication, String archiveHash) throws IOException {
        if (BooksRepository.isArchiveHash(archiveHash)) {
            IBaseBookItem mangaItem = BooksRepository.getBookDetails(archiveHash);
            File archiveFile = new File(mangaItem.getFolder());

            if (!archiveFile.exists()) {
                throw new FileNotFoundException();
            }

            if (!isNeedCheckDownloadAuthority(archiveFile) || UsersDetailsService.isUserCanDownloadFiles(authentication)) {
                writeResponseStream(response, archiveFile);
            } else {
                throw new DownloadsNotAllowedException();
            }
        } else {
            throw new NoReadableFoundException();
        }
    }

    private static boolean isNeedCheckDownloadAuthority(File file) {
        BookType bookType = ContentDetector.detectBookType(file.toPath());
        if (bookType != BookType.ARCHIVE) {
            try {
                RendererFactory.create(bookType, file.getAbsolutePath());
            } catch (RendererNotImplementedException ex) {
                return false;
            }
        }
        return true;
    }

    public static String safeProbeContentType(String path) {
        try {
            return Optional.ofNullable(Files.probeContentType(Paths.get(path)))
                    .orElseGet(() -> MIME_TYPES.get(FileUtils.getFileExt(path).toLowerCase()));
        } catch (Exception ex) {
            return Constants.MimeTypes.IMAGE_JPEG;
        }
    }

    public static boolean writeEntryStreamIntoResponseOrOutputStream(Logger logger, @Nullable HttpServletResponse response, OutputStream outputStream, InputStream entryInputStream,
                                                                     long contentLength, String pagePath, boolean convertImage, long timeStart) {
        TikaInputStream tikaInputStream = null;
        try {
            setResponseContentTypeAndLength(response, FilesHelper.safeProbeContentType(pagePath), (int) contentLength);

            if (convertImage) {
                tikaInputStream = ContentDetector.createTikaInputStream(entryInputStream);
                if (ContentDetector.isWebP(tikaInputStream)) {
                    BufferedImage bufferedImage = ImageIO.read(tikaInputStream);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, Constants.Formats.JPEG, os);
                    setResponseContentTypeAndLength(response, Constants.MimeTypes.IMAGE_JPEG, os.size());
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
            FileUtils.closeLoudly(tikaInputStream);
            FileUtils.closeLoudly(entryInputStream);
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
