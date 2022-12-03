package com.atsumeru.web.renderer;

import com.atsumeru.web.manager.AtsumeruCacheManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class PDFRenderer extends AbstractRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PDFRenderer.class.getSimpleName());

    private final String filePath;

    public static PDFRenderer create(String filePath) {
        return new PDFRenderer(filePath);
    }

    private PDFRenderer(String filePath) {
        this.filePath = filePath;
    }

    public PDDocument getDocument() {
        return AtsumeruCacheManager.PDF_CACHE.get(new File(filePath), PDFRenderer::load);
    }

    public PDDocument getDocumentNonCacheable() {
        return PDFRenderer.load(new File(filePath));
    }

    @Override
    public double getScaleOrDpi() {
        return 300;
    }

    @Override
    public BufferedImage renderPage(int pageIndex, double dpi) {
        return Optional.ofNullable(getDocument())
                .map(document -> renderPage(document, pageIndex - 1, dpi))
                .orElse(null);
    }

    private BufferedImage renderPage(PDDocument document, int pageIndex, double dpi) {
        try {
            org.apache.pdfbox.rendering.PDFRenderer pdfRenderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(pageIndex, (int) dpi, ImageType.RGB);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private static PDDocument load(File file) {
        try {
            return PDDocument.load(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
