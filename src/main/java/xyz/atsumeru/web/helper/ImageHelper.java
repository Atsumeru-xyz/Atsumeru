package xyz.atsumeru.web.helper;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageHelper {

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }

        // Create a buffered image with transparency
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bufferedImage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bufferedImage;
    }
}
