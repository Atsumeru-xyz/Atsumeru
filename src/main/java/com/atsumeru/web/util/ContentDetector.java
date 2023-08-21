package com.atsumeru.web.util;

import com.atsumeru.web.enums.BookType;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ContentDetector {
    private static TikaConfig tika;
    private static final List<String> REPACKABLE_ARCHIVES = Arrays.asList(
            "application/zip",
            "application/x-rar-compressed",
            "application/x-rar-compressed; version=4",
            "application/x-7z-compressed"
    );

    private static final List<String> BOOK_FILES = Arrays.asList(
            "application/epub",
            "application/epub+zip",
            "application/x-fictionbook",
            "application/x-fictionbook+xml",
            "application/pdf",
            "image/vnd.djvu"
    );

    public static final String WEBP_MIME_TYPE = "image/webp";

    public static TikaInputStream createTikaInputStream(InputStream stream) {
        return TikaInputStream.get(stream);
    }

    public static String detectMediaType(Path path) {
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, path.getFileName().toString());

        TikaInputStream tikaInputStream = null;
        try {
            return tika.getDetector().detect(tikaInputStream = TikaInputStream.get(path), metadata).toString();
        } catch (Exception ex) {
            return "unknown";
        } finally {
            FileUtils.closeQuietly(tikaInputStream);
        }
    }

    public static String detectMediaType(TikaInputStream tikaInputStream) {
        try {
            return tika.getDetector().detect(tikaInputStream, new Metadata()).toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    public static BookType detectBookType(Path path) {
        String mediaType = detectMediaType(path);
        switch (mediaType) {
            case "application/zip":
            case "application/x-rar-compressed":
            case "application/x-rar-compressed; version=4":
            case "application/x-7z-compressed":
                return BookType.ARCHIVE;
            case "application/epub":
            case "application/epub+zip":
                return BookType.EPUB;
            case "application/x-fictionbook":
            case "application/x-fictionbook+xml":
                return BookType.FB2;
            case "application/pdf":
                return BookType.PDF;
            case "image/vnd.djvu":
                return BookType.DJVU;
        }
        return null;
    }

    public static boolean isRepackableArchive(Path path) {
        return REPACKABLE_ARCHIVES.contains(detectMediaType(path));
    }

    public static boolean isBookFile(Path path) {
        return BOOK_FILES.contains(detectMediaType(path));
    }

    public static boolean isWebP(Path path) {
        return StringUtils.equalsIgnoreCase(detectMediaType(path), WEBP_MIME_TYPE);
    }

    public static boolean isWebP(TikaInputStream tikaInputStream) {
        return StringUtils.equalsIgnoreCase(detectMediaType(tikaInputStream), WEBP_MIME_TYPE);
    }

    static {
        try {
            tika = new TikaConfig();
        } catch (TikaException | IOException e) {
            e.printStackTrace();
        }
    }
}