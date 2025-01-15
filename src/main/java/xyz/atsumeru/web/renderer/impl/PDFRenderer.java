package xyz.atsumeru.web.renderer.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.atsumeru.web.manager.cache.AtsumeruRenderersCache;
import xyz.atsumeru.web.renderer.AbstractRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class PDFRenderer extends AbstractRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PDFRenderer.class.getSimpleName());

    private final String filePath;
    private PDDocument nonCacheableDocument;

    public static PDFRenderer create(String filePath) {
        return new PDFRenderer(filePath);
    }

    private PDFRenderer(String filePath) {
        this.filePath = filePath;
    }

    public PDDocument getDocument() {
        return Optional.ofNullable(nonCacheableDocument).orElseGet(() -> AtsumeruRenderersCache.getPDDocument(filePath, PDFRenderer::load));
    }

    public PDDocument getDocumentNonCacheable() {
        return nonCacheableDocument = PDFRenderer.load(new File(filePath));
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
