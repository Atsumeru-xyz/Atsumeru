package xyz.atsumeru.web.manager.cache;

import com.djvu2image.DjVuBook;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.pdfbox.pdmodel.PDDocument;
import xyz.atsumeru.web.util.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AtsumeruRenderersCache {
    private static final Cache<File, PDDocument> PDF_CACHE = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener((file, pdf, cause) -> FileUtils.closeLoudly((Closeable) pdf))
            .build();

    private static final Cache<File, DjVuBook> DJVU_CACHE = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    public static PDDocument getPDDocument(String filePath, Function<File, PDDocument> bookFunction) {
        return PDF_CACHE.get(new File(filePath), bookFunction);
    }

    public static DjVuBook getDjvuBook(String filePath, Function<File, DjVuBook> bookFunction) {
        return DJVU_CACHE.get(new File(filePath), bookFunction);
    }
}
