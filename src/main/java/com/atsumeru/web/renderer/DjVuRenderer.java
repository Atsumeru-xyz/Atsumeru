package com.atsumeru.web.renderer;

import com.djvu2image.DjVuBook;
import com.djvu2image.PaperFormat;
import com.atsumeru.web.helper.ImageHelper;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class DjVuRenderer extends AbstractRenderer {
    private static final Logger logger = LoggerFactory.getLogger(DjVuRenderer.class.getSimpleName());

    private final String filePath;

    public static DjVuRenderer create(String filePath) {
        return new DjVuRenderer(filePath);
    }

    private DjVuRenderer(String filePath) {
        this.filePath = filePath;
    }

    public DjVuBook getBook() {
        return AtsumeruCacheManager.DJVU_CACHE.get(new File(filePath), DjVuRenderer::load);
    }

    @Override
    public double getScaleOrDpi() {
        return 2.0;
    }

    @Override
    public BufferedImage renderPage(int pageIndex, double scale) {
        return Optional.ofNullable(getBook())
                .map(book -> book.getPageImage(pageIndex, false, scale))
                .map(ImageHelper::toBufferedImage)
                .orElse(null);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private static DjVuBook load(File file) {
        try {
            return DjVuBook.open(file, PaperFormat.A4, false);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
