package xyz.atsumeru.web.io.image;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public interface ImageOutStreamWriter {

    default void write(File outputFile, int page) {
        try (OutputStream out = new FileOutputStream(outputFile)) {
            write(null, out, page, false);
        } catch (Exception ex) {
            outputFile.delete();
        }
    }

    void write(@Nullable HttpServletResponse response, OutputStream responseOut, int page, boolean convertImage);
}
