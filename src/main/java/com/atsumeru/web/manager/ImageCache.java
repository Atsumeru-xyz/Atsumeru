package com.atsumeru.web.manager;

import com.atsumeru.web.exception.NoCoverFoundException;
import com.atsumeru.web.helper.ArchiveHelper;
import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.helper.FilesHelper;
import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.image.Images;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.util.WorkspaceUtils;
import lombok.Getter;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCache {
    private ImageCache() {
    }

    public static boolean isInCache(String imageHash, ImageCacheType cacheType) {
        return getImage(imageHash, cacheType).exists();
    }

    public static File getImage(String imageHash, ImageCacheType cacheType) {
        return getImage(imageHash, Constants.PNG, cacheType);
    }

    public static File getImage(String imageHash, String extension, ImageCacheType cacheType) {
        return new File(WorkspaceUtils.CACHE_DIR + cacheType.getFolder(), String.format("%s.%s", imageHash, extension));
    }

    public static byte[] getImageBytesFromCache(String imageHash, ImageCache.ImageCacheType cacheType) {
        File image = ImageCache.getImage(imageHash, cacheType);
        try (FileInputStream fis = new FileInputStream(image)) {
            return IOUtils.toByteArray(fis);
        } catch (IOException e) {
//            return saveAndGetImageBytesIntoCache(imageHash, cacheType);
            throw new NoCoverFoundException();
        }
    }

    private static byte[] saveAndGetImageBytesIntoCache(String imageHash, ImageCache.ImageCacheType cacheType) {
        if (cacheType == ImageCache.ImageCacheType.THUMBNAIL) {
            if (saveImageIntoCache(imageHash)) {
                return ImageCache.getImageBytesFromCache(imageHash, cacheType);
            }
        }
        throw new NoCoverFoundException();
    }


    public static boolean saveImageIntoCache(String imageHash) {
        return BooksRepository.isChapterHash(imageHash)
                ? saveChapterImageIntoCache(imageHash)
                : FilesHelper.saveBookImageIntoCache(imageHash);
    }

    private static boolean saveChapterImageIntoCache(String imageHash) {
        BookChapter chapter = BooksRepository.getChapter(imageHash);
        File originalImage = ImageCache.getImage(imageHash, "tmp", ImageCache.ImageCacheType.THUMBNAIL);
        ArchiveHelper.saveBookPage(originalImage, chapter.getArchiveId(), imageHash, 1);

        try {
            createThumbnail(ImageIO.read(originalImage), ImageCache.getImage(imageHash, ImageCache.ImageCacheType.THUMBNAIL));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        originalImage.delete();
        return true;
    }


    public static Images saveToFile(InputStream inputStream, String imageHash, String extension) {
        String imageName = String.format("%s.%s", imageHash, extension);

        File thumbnailFolder = new File(WorkspaceUtils.CACHE_DIR + ImageCacheType.THUMBNAIL.getFolder());
        File thumbnailImage = new File(thumbnailFolder, imageName);
        thumbnailFolder.mkdirs();

        BufferedImage bImage = null;
        try {
            createThumbnail(bImage = ImageIO.read(inputStream), thumbnailImage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            GUFile.closeQuietly(inputStream);
        }

        return new Images(thumbnailImage.getPath(), bImage);
    }

    private static void createThumbnail(BufferedImage image, File thumbnailImage) {
        try {
            Thumbnails.of(image)
                    .scalingMode(ScalingMode.PROGRESSIVE_BILINEAR)
                    .size(230, 320)
                    .toFile(thumbnailImage);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public enum ImageCacheType {
        ORIGINAL("original"),
        THUMBNAIL("thumbnail");

        @Getter
        private String folder;

        ImageCacheType(String folder) {
            this.folder = folder;
        }
    }
}