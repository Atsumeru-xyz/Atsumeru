package com.atsumeru.web.controller.rest;

import com.atsumeru.web.AtsumeruApplication;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.service.CoversSaverService;
import com.atsumeru.web.helper.JavaHelper;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.ServerInfo;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.util.WorkspaceUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping(ServerApiController.ROOT_ENDPOINT)
public class ServerApiController {
    protected static final String ROOT_ENDPOINT = "/api/server";
    private static final String PING_ENDPOINT = "/ping";

    private static final BooksDaoManager daoManager;

    static {
        daoManager = BooksDatabaseRepository.getInstance().getDaoManager();
    }

    public static String getPingEndpoint() {
        return ROOT_ENDPOINT + PING_ENDPOINT;
    }

    @GetMapping(value = PING_ENDPOINT)
    public ResponseEntity<HttpStatus> ping() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ServerInfo info() {
        return new ServerInfo()
                .setName("Atsumeru")
                .setVersion(JavaHelper.getAppVersion(AtsumeruApplication.class))
                .setVersionName("Bohrium")
                .setHasPassword(true)
                .setDebugMode(JavaHelper.isDebug())
                .setStats(new ServerInfo.Stats()
                        .setTotalSeries(daoManager.count(BookSerie.class))
                        .setTotalArchives(daoManager.count(BookArchive.class))
                        .setTotalChapters(daoManager.count(BookChapter.class))
                        .setTotalCategories(daoManager.count(Category.class)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/clear_cover_cache")
    public ResponseEntity<AtsumeruMessage> clearCache() {
        new Thread(() -> {
            GUFile.deleteDirectory(new File(WorkspaceUtils.CACHE_DIR));
            WorkspaceUtils.configureWorkspace();
            CoversSaverService.saveNonExistentCoversIntoCache();
        }).start();

        return RestHelper.createResponseMessage("Cache cleared", HttpStatus.OK);
    }
}
