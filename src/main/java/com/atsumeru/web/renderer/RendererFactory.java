package com.atsumeru.web.renderer;

import com.atsumeru.web.exception.RendererNotImplementedException;
import com.atsumeru.web.enums.BookType;
import org.springframework.lang.Nullable;

public class RendererFactory {

    public static AbstractRenderer create(@Nullable BookType bookType, String filePath) {
        if (bookType != null) {
            switch (bookType) {
                case PDF:
                    return PDFRenderer.create(filePath);
                case DJVU:
                    return DjVuRenderer.create(filePath);
            }
        }
        throw new RendererNotImplementedException("Renderer for type " + bookType + " not yet implemented!");
    }
}
