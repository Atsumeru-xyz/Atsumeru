package xyz.atsumeru.web.controller.rest.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.helper.FilesHelper;
import xyz.atsumeru.web.manager.ImageCache;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Files", description = "API for requesting files from server")
public class FilesApiController {

    @Operation(summary = "Download Volume", description = "Download Volume archive file by Volume Hash")
    @GetMapping("/download/{archive_hash}")
    public void downloadBook(HttpServletResponse response,
                             @PathVariable(value = "archive_hash") String archiveHash) throws IOException {
        FilesHelper.downloadFile(response, SecurityContextHolder.getContext().getAuthentication(), archiveHash);
    }

    @Operation(summary = "Book Cover", description = "Get Book cover image by Cover Hash with optional converting to PNG")
    @GetMapping(value = "/cover/{image_hash}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBookCover(HttpServletResponse response,
                                             @PathVariable(value = "image_hash") String imageHash,
                                             @RequestParam(value = "type", defaultValue = "original") ImageCache.ImageCacheType imageCacheType,
                                             @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) {
        return FilesHelper.getCover(response, imageHash, imageCacheType, convertImage);
    }
}
