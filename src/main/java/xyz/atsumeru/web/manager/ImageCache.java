package xyz.atsumeru.web.manager;

import lombok.Getter;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;
import org.apache.commons.io.IOUtils;
import xyz.atsumeru.web.exception.NoCoverFoundException;
import xyz.atsumeru.web.helper.Constants;
import xyz.atsumeru.web.helper.FilesHelper;
import xyz.atsumeru.web.io.image.ImageOutStreamWriterFactory;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.book.image.Images;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.util.FileUtils;

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
        return getImage(imageHash, Constants.Formats.PNG, cacheType);
    }

    public static File getImage(String imageHash, String extension, ImageCacheType cacheType) {
        return new File(new File(Workspace.CACHE_DIR, cacheType.getFolder()), String.format("%s.%s", imageHash, extension));
    }

    public static byte[] getImageBytesFromCache(String imageHash, ImageCache.ImageCacheType cacheType) {
        File image = ImageCache.getImage(imageHash, cacheType);
        try (FileInputStream fis = new FileInputStream(image)) {
            return IOUtils.toByteArray(fis);
        } catch (IOException e) {
            throw new NoCoverFoundException();
        }
    }

    public static boolean saveImageIntoCache(String imageHash) {
        return BooksRepository.isChapterHash(imageHash)
                ? saveChapterImageIntoCache(imageHash)
                : FilesHelper.saveBookImageIntoCache(imageHash);
    }

    private static boolean saveChapterImageIntoCache(String imageHash) {
        BookChapter chapter = BooksRepository.getChapter(imageHash);
        File originalImage = ImageCache.getImage(imageHash, "tmp", ImageCache.ImageCacheType.THUMBNAIL);
        ImageOutStreamWriterFactory.create(chapter.getArchiveId(), imageHash).write(originalImage, 1);

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

        File thumbnailFolder = new File(Workspace.CACHE_DIR, ImageCacheType.THUMBNAIL.getFolder());
        File thumbnailImage = new File(thumbnailFolder, imageName);
        thumbnailFolder.mkdirs();

        BufferedImage bImage = null;
        try {
            createThumbnail(bImage = ImageIO.read(inputStream), thumbnailImage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeLoudly(inputStream);
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