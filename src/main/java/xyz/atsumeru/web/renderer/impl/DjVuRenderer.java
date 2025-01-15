package xyz.atsumeru.web.renderer.impl;

import com.djvu2image.DjVuBook;
import com.djvu2image.PaperFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.atsumeru.web.helper.ImageHelper;
import xyz.atsumeru.web.manager.cache.AtsumeruRenderersCache;
import xyz.atsumeru.web.renderer.AbstractRenderer;

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
        return AtsumeruRenderersCache.getDjvuBook(filePath, DjVuRenderer::load);
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
