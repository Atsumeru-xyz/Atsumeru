package com.atsumeru.web.renderer;

import com.atsumeru.web.exception.DjVuReadingException;
import com.atsumeru.web.util.FileUtils;
import org.slf4j.Logger;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public abstract class AbstractRenderer {

    public abstract BufferedImage renderPage(int pageIndex, double scaleOrDpi);

    public abstract double getScaleOrDpi();

    public abstract Logger getLogger();

    public boolean renderPage(OutputStream outputStream, int pageIndex, double scale) {
        BufferedImage bim = renderPage(pageIndex, scale);
        if (bim != null) {
            try {
                ImageIO.write(bim, "jpeg", outputStream);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void renderPage(@Nullable HttpServletResponse response, OutputStream outputStream, int page) {
        long time = System.currentTimeMillis();
        Optional.ofNullable(renderPage(page, getScaleOrDpi()))
                .map(img -> writeBufferedImageIntoResponseOrOutputStream(response, outputStream, img, time))
                .orElseThrow(DjVuReadingException::new);
    }

    private boolean writeBufferedImageIntoResponseOrOutputStream(@Nullable HttpServletResponse response, OutputStream outputStream,
                                                                 BufferedImage bufferedImage, long timeStart) {
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpeg", tmp);
            FileUtils.closeQuietly(tmp);

            int contentLength = tmp.size();
            if (response != null) {
                response.setContentType("image/jpeg");
                response.setContentLength(contentLength);
            }
            ImageIO.write(bufferedImage, "jpeg", outputStream);
            if (response != null) {
                getLogger().info("Image unpacking and writing time: " + (System.currentTimeMillis() - timeStart) + "ms. Image length: " + contentLength + " bytes");
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
