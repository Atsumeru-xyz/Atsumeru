package com.atsumeru.web.model.book.image;

import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

public class Images {
    @Getter @Setter private String thumbnail;
    @Getter @Setter private String accent;
    @Getter @Setter private BufferedImage originalBufferedImage;

    public Images(String thumbnail, BufferedImage bufferedImage) {
        this.thumbnail = thumbnail;
        this.originalBufferedImage = bufferedImage;
    }
}