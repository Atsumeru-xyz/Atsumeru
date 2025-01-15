package xyz.atsumeru.web.io.image.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import xyz.atsumeru.web.enums.BookType;
import xyz.atsumeru.web.helper.Constants;
import xyz.atsumeru.web.io.image.ImageOutStreamWriter;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.renderer.AbstractRenderer;
import xyz.atsumeru.web.renderer.RendererFactory;
import xyz.atsumeru.web.util.ContentDetector;
import xyz.atsumeru.web.util.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

public class RenderedImageOutStreamWriter implements ImageOutStreamWriter {
    private static final Logger logger = LoggerFactory.getLogger(RenderedImageOutStreamWriter.class.getSimpleName());

    private final IBaseBookItem baseBookItem;

    public RenderedImageOutStreamWriter(IBaseBookItem baseBookItem) {
        this.baseBookItem = baseBookItem;
    }

    @Override
    public void write(@Nullable HttpServletResponse response, OutputStream responseOut, int page, boolean convertImage) {
        long time = System.currentTimeMillis();

        BookType bookType = ContentDetector.detectBookType(Paths.get(baseBookItem.getFolder()));
        AbstractRenderer renderer = RendererFactory.create(bookType, baseBookItem.getFolder());
        BufferedImage bufferedImage = renderer.renderPage(page, renderer.getScaleOrDpi());
        writeBufferedImageIntoResponseOrOutputStream(response, responseOut, bufferedImage, time);
    }

    private void writeBufferedImageIntoResponseOrOutputStream(@Nullable HttpServletResponse response, OutputStream outputStream,
                                                                 BufferedImage bufferedImage, long timeStart) {
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, Constants.Formats.JPEG, tmp);
            FileUtils.closeLoudly(tmp);

            int contentLength = tmp.size();
            if (response != null) {
                response.setContentType(Constants.MimeTypes.IMAGE_JPEG);
                response.setContentLength(contentLength);
            }
            ImageIO.write(bufferedImage, Constants.Formats.JPEG, outputStream);
            if (response != null) {
                logger.info("Image unpacking and writing time: " + (System.currentTimeMillis() - timeStart) + "ms. Image length: " + contentLength + " bytes");
            }
        } catch (IOException ex) {
            logger.error("Unable to write image", ex);
        }
    }
}
