package com.atsumeru.web.controller.rest.settings;

import com.atsumeru.web.AtsumeruApplication;
import com.atsumeru.web.configuration.FileWatcherConfig;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.interceptor.RequestLogInterceptor;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.settings.ServerSettings;
import com.atsumeru.web.service.BonjourService;
import com.atsumeru.web.service.CoversSaverService;
import com.atsumeru.web.service.ImportService;
import com.atsumeru.web.service.MetadataUpdateService;
import com.atsumeru.web.util.AppUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsApiController {
    private final Supplier<Boolean> serverLockedSupplier = () -> MetadataUpdateService.isUpdateActive() || ImportService.isImportActive() || CoversSaverService.isCachingActive();

    private final ApplicationContext context;

    public SettingsApiController(ApplicationContext context) {
        this.context = context;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getSettings() {
        if (serverLockedSupplier.get()) {
            return RestHelper.createResponseMessage("Unable to edit server settings", HttpStatus.NOT_ACCEPTABLE);
        }
        return new ResponseEntity<>(
                new ServerSettings(
                        Settings.isAllowListLoadingWithVolumes(),
                        Settings.isAllowListLoadingWithChapters(),
                        Settings.isDisableRequestLoggingIntoConsole(),
                        Settings.isDisableBonjourService(),
                        Settings.isDisableFileWatcher(),
                        Settings.isDisableWatchForModifiedFiles(),
                        Settings.isDisableChapters()
                ),
                HttpStatus.OK
        );
    }

    @PostMapping("/update")
    public ResponseEntity<AtsumeruMessage> updateSettings(@RequestBody ServerSettings settings) {
        boolean currentDisableChapters = Settings.isDisableChapters();

        Settings.putAllowListLoadingWithVolumes(settings.isAllowLoadingListWithVolumes());
        Settings.putAllowListLoadingWithChapters(settings.isAllowLoadingListWithChapters());
        Settings.putDisableRequestLoggingIntoConsole(settings.isDisableRequestLoggingIntoConsole());
        Settings.putDisableBonjourService(settings.isDisableBonjourService());
        Settings.putDisableFileWatcher(settings.isDisableFileWatcher());
        Settings.putDisableWatchForModifiedFiles(settings.isDisableWatchForModifiedFiles());
        Settings.putDisableChapters(settings.isDisableChapters());

        context.getBean(BonjourService.class).restartService();
        context.getBean(RequestLogInterceptor.class).onSettingsUpdate();
        FileWatcherConfig.start();

        if (currentDisableChapters != settings.isDisableChapters()) {
            restartServerDelayed();
        }

        return RestHelper.createResponseMessage("Settings updated successfully", HttpStatus.OK);
    }

    private void restartServerDelayed() {
        new Thread(() -> {
            AppUtils.sleepWhile(1000, serverLockedSupplier);
            AtsumeruApplication.restart();
        }).start();
    }
}
