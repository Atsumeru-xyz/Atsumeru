package xyz.atsumeru.web.controller.rest.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.exception.NoReadableFoundException;
import xyz.atsumeru.web.exception.SharingTokenExpiredException;
import xyz.atsumeru.web.exception.SharingTokenNotFoundException;
import xyz.atsumeru.web.helper.FilesHelper;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.io.image.ImageOutStreamWriterFactory;
import xyz.atsumeru.web.manager.ImageCache;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.book.volume.VolumeItem;
import xyz.atsumeru.web.model.database.AtsumeruUser;
import xyz.atsumeru.web.model.database.ShareToken;
import xyz.atsumeru.web.model.share.CreateShareTokenRequest;
import xyz.atsumeru.web.repository.BooksRepository;
import xyz.atsumeru.web.repository.dao.UsersDaoManager;
import xyz.atsumeru.web.util.ArrayUtils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RestController
@RequestMapping("/api/v1/share")
@Tag(name = "Sharing", description = "API for managing share tokens and accessing shared content")
public class SharingApiController {

    private final UsersDaoManager usersDaoManager;

    public SharingApiController(UsersDaoManager usersDaoManager) {
        this.usersDaoManager = usersDaoManager;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create share token", description = "Create a new share token for sharing book content. Admin only.")
    public ShareToken createShareToken(@RequestBody CreateShareTokenRequest request) {
        ShareToken token = new ShareToken();
        token.setToken(UUID.randomUUID().toString());
        token.setName(request.getName());
        token.setSerieHash(request.getSerieHash());
        token.setSerieName(resolveSerieName(request.getSerieHash()));
        token.setAuthorities(request.getAuthorities());
        token.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        token.setCreatedAt(System.currentTimeMillis());
        token.setExpiresAt(request.getExpiresAt());
        usersDaoManager.saveShareToken(token);
        return token;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List share tokens", description = "Get list of all share tokens. Admin only.")
    public List<ShareToken> listShareTokens() {
        return usersDaoManager.queryAllShareTokens();
    }

    @DeleteMapping("/{token}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete share token", description = "Delete a share token by its value. Admin only.")
    public ResponseEntity<AtsumeruMessage> deleteShareToken(@PathVariable(value = "token") String token) {
        ShareToken shareToken = usersDaoManager.queryShareToken(token);
        if (shareToken == null) {
            throw new SharingTokenNotFoundException();
        }
        usersDaoManager.deleteShareToken(shareToken);
        return RestHelper.createResponseMessage("Share token deleted", HttpStatus.OK);
    }

    @GetMapping("/{token}")
    @Operation(summary = "Shared book details", description = "Get book details associated with the share token")
    public IBaseBookItem getSharedBookDetails(@PathVariable(value = "token") String token,
                                              @RequestParam(value = "with_volumes", defaultValue = "false") boolean withVolumesAndHistory,
                                              @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        ShareToken shareToken = getAndValidateShareToken(token);
        AtsumeruUser shareUser = createShareUser();
        return BooksRepository.getBookDetails(shareUser, shareToken.getSerieHash(), withVolumesAndHistory, withChapters);
    }

    @GetMapping("/{token}/volumes")
    @Operation(summary = "Shared book volumes", description = "Get volume list for shared book")
    public List<VolumeItem> getSharedVolumes(@PathVariable(value = "token") String token,
                                             @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        ShareToken shareToken = getAndValidateShareToken(token);
        AtsumeruUser shareUser = createShareUser();
        return ArrayUtils.getNotNullList(BooksRepository.getBookDetails(shareUser, shareToken.getSerieHash(), true, withChapters).getVolumes());
    }

    @GetMapping("/{token}/volumes/{archive_hash}")
    @Operation(summary = "Shared volume details", description = "Get volume details accessible via share token")
    public VolumeItem getSharedVolume(@PathVariable(value = "token") String token,
                                      @PathVariable(value = "archive_hash") String archiveHash,
                                      @RequestParam(value = "with_chapters", defaultValue = "false") boolean withChapters) {
        ShareToken shareToken = getAndValidateShareToken(token);
        validateArchiveBelongsToSerie(shareToken, archiveHash);
        AtsumeruUser shareUser = createShareUser();
        return ArrayUtils.getNotNullList(BooksRepository.getBookDetails(shareUser, archiveHash, true, withChapters).getVolumes()).get(0);
    }

    @GetMapping(value = {
            "/{token}/{archive_or_chapter_hash}/page/{page}",
            "/{token}/volumes/{archive_hash}/page/{page}"
    })
    @Operation(summary = "Shared page image", description = "Get page image from a shared volume")
    public void getSharedPage(HttpServletResponse response,
                              @PathVariable(value = "token") String token,
                              @PathVariable(value = "archive_or_chapter_hash", required = false) String archiveOrChapterHash,
                              @PathVariable(value = "archive_hash", required = false) String archiveHash,
                              @PathVariable(value = "page") int page,
                              @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) throws IOException {
        ShareToken shareToken = getAndValidateShareToken(token);
        String hashToCheck = archiveOrChapterHash != null ? archiveOrChapterHash : archiveHash;
        validateArchiveBelongsToSerie(shareToken, hashToCheck);
        ImageOutStreamWriterFactory.create(
                !BooksRepository.isChapterHash(archiveOrChapterHash)
                        ? archiveOrChapterHash
                        : archiveHash,
                BooksRepository.isChapterHash(archiveOrChapterHash)
                        ? archiveOrChapterHash
                        : null
        ).write(response, response.getOutputStream(), page, convertImage);
    }

    @GetMapping("/{token}/download/{archive_hash}")
    @Operation(summary = "Shared volume download", description = "Download shared volume archive file (requires DOWNLOAD_FILES authority)")
    public void downloadSharedBook(HttpServletResponse response,
                                   @PathVariable(value = "token") String token,
                                   @PathVariable(value = "archive_hash") String archiveHash) throws IOException {
        ShareToken shareToken = getAndValidateShareToken(token);
        validateArchiveBelongsToSerie(shareToken, archiveHash);

        if (!shareToken.getAuthoritiesSet().contains("DOWNLOAD_FILES")) {
            throw new NoReadableFoundException();
        }

        FilesHelper.downloadFile(response, SecurityContextHolder.getContext().getAuthentication(), archiveHash);
    }

    @GetMapping("/{token}/cover/{image_hash}")
    @Operation(summary = "Shared book cover", description = "Get cover image from shared book")
    @ResponseBody
    public byte[] getSharedBookCover(HttpServletResponse response,
                                     @PathVariable(value = "token") String token,
                                     @PathVariable(value = "image_hash") String imageHash,
                                     @RequestParam(value = "type", defaultValue = "original") ImageCache.ImageCacheType imageCacheType,
                                     @RequestParam(value = "convert", defaultValue = "false") boolean convertImage) {
        ShareToken shareToken = getAndValidateShareToken(token);
        if (!isCoverBelongsToSerie(shareToken, imageHash)) {
            throw new NoReadableFoundException();
        }
        return FilesHelper.getCover(response, imageHash, imageCacheType, convertImage);
    }

    private ShareToken getAndValidateShareToken(String token) {
        ShareToken shareToken = usersDaoManager.queryShareToken(token);
        if (shareToken == null) {
            throw new SharingTokenNotFoundException();
        }
        if (shareToken.getExpiresAt() != null && shareToken.getExpiresAt() <= System.currentTimeMillis()) {
            throw new SharingTokenExpiredException();
        }
        return shareToken;
    }

    private void validateArchiveBelongsToSerie(ShareToken shareToken, String archiveHash) {
        List<IBaseBookItem> serieList = Beans.getBooksDaoManager().query(shareToken.getSerieHash(), BookSerie.class);
        if (ArrayUtils.isEmpty(serieList)) {
            throw new NoReadableFoundException();
        }
        Long serieDbId = serieList.get(0).getDbId();

        List<IBaseBookItem> archiveList = Beans.getBooksDaoManager().query(archiveHash, BookArchive.class);
        if (ArrayUtils.isEmpty(archiveList)) {
            throw new NoReadableFoundException();
        }
        IBaseBookItem archive = archiveList.get(0);

        if (!serieDbId.equals(archive.getSerieDbId())) {
            throw new NoReadableFoundException();
        }
    }

    private static boolean isCoverBelongsToSerie(ShareToken shareToken, String imageHash) {
        if (imageHash.equals(shareToken.getSerieHash())) {
            return true;
        }
        List<IBaseBookItem> serieList = Beans.getBooksDaoManager().query(shareToken.getSerieHash(), BookSerie.class);
        if (ArrayUtils.isEmpty(serieList)) {
            return false;
        }
        IBaseBookItem serie = serieList.get(0);
        String coverHash = serie.getCover();
        if (coverHash != null && coverHash.contains("?")) {
            coverHash = coverHash.substring(0, coverHash.indexOf('?'));
        }
        if (imageHash.equals(coverHash)) {
            return true;
        }
        if (BooksRepository.isArchiveHash(imageHash)) {
            Long serieDbId = serie.getDbId();
            List<IBaseBookItem> archiveList = Beans.getBooksDaoManager().query(imageHash, BookArchive.class);
            if (ArrayUtils.isNotEmpty(archiveList)) {
                return serieDbId.equals(archiveList.get(0).getSerieDbId());
            }
        }
        return false;
    }

    private static AtsumeruUser createShareUser() {
        AtsumeruUser user = new AtsumeruUser();
        user.setId(0L);
        user.setAuthorities("");
        user.setRoles("");
        user.setAllowedCategories("");
        user.setDisallowedGenres("");
        user.setDisallowedTags("");
        return user;
    }

    private static String resolveSerieName(String serieHash) {
        List<IBaseBookItem> serieList = Beans.getBooksDaoManager().query(serieHash, BookSerie.class);
        if (ArrayUtils.isNotEmpty(serieList)) {
            return serieList.get(0).getTitle();
        }
        return null;
    }
}
