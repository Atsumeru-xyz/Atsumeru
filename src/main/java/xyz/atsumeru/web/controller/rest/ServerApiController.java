package xyz.atsumeru.web.controller.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.helper.JavaHelper;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.manager.Workspace;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.ServerInfo;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.chapter.BookChapter;
import xyz.atsumeru.web.model.database.Category;
import xyz.atsumeru.web.service.CoversSaverService;
import xyz.atsumeru.web.util.FileUtils;

@RestController
@RequestMapping(ServerApiController.ROOT_ENDPOINT)
@Tag(name = "Server", description = "Server specific API: ping, get server info, clear covers cache")
public class ServerApiController {
    protected static final String ROOT_ENDPOINT = "/api/server";
    private static final String PING_ENDPOINT = "/ping";

    public static String getPingEndpoint() {
        return ROOT_ENDPOINT + PING_ENDPOINT;
    }

    @Operation(summary = "Ping server", description = "May be used to check if server is online")
    @GetMapping(value = PING_ENDPOINT)
    public ResponseEntity<HttpStatus> ping() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get server info", description = "Get server info: version, name and series, volumes, chapters, categories stats")
    @GetMapping("/info")
    public ServerInfo info() {
        return new ServerInfo()
                .setName("Atsumeru")
                .setVersion(JavaHelper.getAppVersion(AtsumeruApplication.class))
                .setVersionName("Bohrium")
                .setHasPassword(true)
                .setDebugMode(JavaHelper.isDebug())
                .setStats(new ServerInfo.Stats()
                        .setTotalSeries(Beans.getBooksDaoManager().count(BookSerie.class))
                        .setTotalArchives(Beans.getBooksDaoManager().count(BookArchive.class))
                        .setTotalChapters(Beans.getBooksDaoManager().count(BookChapter.class))
                        .setTotalCategories(Beans.getBooksDaoManager().count(Category.class)));
    }

    @Operation(summary = "Recreate covers cache", description = "Clear covers cache and start caching service to build new cache")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clear_cover_cache")
    public ResponseEntity<AtsumeruMessage> clearCache() {
        new Thread(() -> {
            FileUtils.deleteDirectory(Workspace.CACHE_DIR);
            Workspace.checkWorkspace();
            CoversSaverService.saveNonExistentCoversIntoCache();
        }).start();

        return RestHelper.createResponseMessage("Cache cleared", HttpStatus.OK);
    }
}
