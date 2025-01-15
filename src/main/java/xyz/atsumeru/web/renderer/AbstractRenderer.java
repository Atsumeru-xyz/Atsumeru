package xyz.atsumeru.web.renderer;

import org.slf4j.Logger;
import xyz.atsumeru.web.helper.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractRenderer {

    public abstract BufferedImage renderPage(int pageIndex, double scaleOrDpi);

    public abstract double getScaleOrDpi();

    public abstract Logger getLogger();

    public boolean renderPage(OutputStream outputStream, int pageIndex, double scale) {
        BufferedImage bim = renderPage(pageIndex, scale);
        if (bim != null) {
            try {
                ImageIO.write(bim, Constants.Formats.JPEG, outputStream);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
