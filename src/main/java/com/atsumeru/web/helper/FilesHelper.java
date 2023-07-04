package com.atsumeru.web.helper;

import com.atsumeru.web.enums.BookType;
import com.atsumeru.web.exception.DownloadsNotAllowedException;
import com.atsumeru.web.exception.NoReadableFoundException;
import com.atsumeru.web.exception.RendererNotImplementedException;
import com.atsumeru.web.manager.ImageCache;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.model.book.image.Images;
import com.atsumeru.web.model.importer.ReadableContent;
import com.atsumeru.web.renderer.RendererFactory;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.service.UserDatabaseDetailsService;
import com.atsumeru.web.util.ContentDetector;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUFile;
import org.apache.commons.io.IOUtils;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(Constants.JPG, "image/jpeg");
        MIME_TYPES.put(Constants.JPEG, "image/jpeg");
        MIME_TYPES.put(Constants.PNG, "image/png");
        MIME_TYPES.put(Constants.GIF, "image/gif");
        MIME_TYPES.put(Constants.WEBP, "image/webp");
        MIME_TYPES.put(Constants.AVIF, "image/avif");
        MIME_TYPES.put(Constants.HEIC, "image/heic");
        MIME_TYPES.put(Constants.HEIF, "image/heif");
    }

    public static String readHashFileAttribute(Path path, String attributeName, String defaultHash) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
                Object attr = Files.getAttribute(path, attributeName, LinkOption.NOFOLLOW_LINKS);
                if (attr instanceof byte[]) {
                    return new String((byte[]) attr);
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
            if (GUArray.isNotEmpty(archivesList)) {
                IBaseBookItem book = archivesList.get(0);

                File externalCover = ReadableContent.getSerieExternalCover(book.getFolder());
                if (externalCover != null) {
                    try {
                        ArchiveHelper.writeEntryStreamIntoResponseOrOutputStream(response, response.getOutputStream(),
                                Files.newInputStream(externalCover.toPath()), externalCover.length(),
                                externalCover.getAbsolutePath(), convertImage, System.currentTimeMillis());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                } else {
                    imageHash = archivesList.get(0).getCover();
                }
            }
        }
        ArchiveHelper.getBookPage(response, imageHash, 1, convertImage);
    }

    public static boolean saveBookImageIntoCache(String imageHash) {
        IBaseBookItem book = BooksRepository.getBookDetails(imageHash);

        String itemHash = imageHash;
        boolean asSingle = book.isSingle();

        if (BooksRepository.isSeriesHash(itemHash)) {
            asSingle = BooksRepository.getBookDetails(itemHash).isSingle();
            List<IBaseBookItem> archivesList = BooksRepository.getArchivesForSerie(imageHash);
            if (GUArray.isNotEmpty(archivesList)) {
                itemHash = archivesList.get(0).getCover();
            }
        }

        BooksDaoManager daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
        List<BookArchive> archives = daoManager.query(itemHash, BookArchive.class);

        if (GUArray.isNotEmpty(archives)) {
            Images images = ReadableContent.saveCoverImage(archives.get(0), asSingle);
            archives.forEach(baseBook -> {
                baseBook.setCoverAccent(Optional.ofNullable(images)
                        .map(Images::getAccent)
                        .orElse(baseBook.getCoverAccent()));
                daoManager.save(baseBook);
            });
        }

        return GUFile.isFileExist(ImageCache.getImage(imageHash, Constants.PNG, ImageCache.ImageCacheType.THUMBNAIL));
    }

    private static void writeResponseStream(HttpServletResponse response, File file) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(file)) {
            response.setContentType(Files.probeContentType(file.toPath()));
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "attachment; filename=" + GUFile.getFileNameWithExt(file.getPath(), true));
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

            if (!isNeedCheckDownloadAuthority(archiveFile) || UserDatabaseDetailsService.isUserCanDownloadFiles(authentication)) {
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
                    .orElseGet(() -> MIME_TYPES.get(GUFile.getFileExt(path).toLowerCase()));
        } catch (Exception ex) {
            return "image/jpeg";
        }
    }
}
