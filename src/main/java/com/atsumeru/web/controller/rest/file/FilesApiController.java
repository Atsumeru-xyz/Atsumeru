package com.atsumeru.web.controller.rest.file;

import com.atsumeru.web.helper.FilesHelper;
import com.atsumeru.web.manager.ImageCache;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class FilesApiController {

    @GetMapping("/download/{archive_hash}")
    public void downloadBook(HttpServletResponse response,
                             @PathVariable(value = "archive_hash") String archiveHash) throws IOException {
        FilesHelper.downloadFile(response, SecurityContextHolder.getContext().getAuthentication(), archiveHash);
    }

    @GetMapping(value = "/cover/{image_hash}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getBookCover(HttpServletResponse response,
                                             @PathVariable(value = "image_hash") String imageHash,
                                             @RequestParam(value = "type", defaultValue = "original") ImageCache.ImageCacheType imageCacheType,
                                             @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) {
        return FilesHelper.getCover(response, imageHash, imageCacheType, convertImage);
    }
}
