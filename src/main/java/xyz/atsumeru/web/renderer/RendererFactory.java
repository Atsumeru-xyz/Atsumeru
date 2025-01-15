package xyz.atsumeru.web.renderer;

import org.springframework.lang.Nullable;
import xyz.atsumeru.web.enums.BookType;
import xyz.atsumeru.web.exception.RendererNotImplementedException;
import xyz.atsumeru.web.renderer.impl.DjVuRenderer;
import xyz.atsumeru.web.renderer.impl.PDFRenderer;

public class RendererFactory {

    public static AbstractRenderer create(@Nullable BookType bookType, String filePath) {
        if (bookType != null) {
            return switch (bookType) {
                case PDF -> PDFRenderer.create(filePath);
                case DJVU -> DjVuRenderer.create(filePath);
                case ARCHIVE, EPUB, FB2 -> throw new RendererNotImplementedException("Renderer for type " + bookType + " not yet implemented!");
            };
        }
        throw new RendererNotImplementedException("Renderer for type [null] not yet implemented!");
    }
}
