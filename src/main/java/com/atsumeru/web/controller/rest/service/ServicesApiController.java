package com.atsumeru.web.controller.rest.service;

import com.atsumeru.web.controller.rest.importer.ImporterApiController;
import com.atsumeru.web.controller.rest.metadata.MetadataApiController;
import com.atsumeru.web.exception.MetadataUpdateActiveException;
import com.atsumeru.web.model.service.ServicesStatus;
import com.atsumeru.web.service.CoversSaverService;
import com.atsumeru.web.service.MetadataUpdateService;
import com.atsumeru.web.service.UserDatabaseDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServicesApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('IMPORTER', 'METADATA_UPDATER')")
public class ServicesApiController {
    protected static final String ROOT_ENDPOINT = "/api/v1/services";
    private static final String STATUS_ENDPOINT = "/status";

    @Autowired
    ImporterApiController importerController;

    @Autowired
    MetadataApiController metadataController;

    public static void checkIsBlockingServicesRunning(boolean isServiceStatusRequest) {
        if (MetadataUpdateService.isUpdateActive() && !isServiceStatusRequest) {
            throw new MetadataUpdateActiveException();
        }
    }

    @GetMapping(STATUS_ENDPOINT)
    public ServicesStatus getStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isUserAdminOrImporter = UserDatabaseDetailsService.isUserInRole(auth, "ADMIN", "IMPORTER");
            return new ServicesStatus(
                    isUserAdminOrImporter ? importerController.getStatus() : null,
                    UserDatabaseDetailsService.isUserInRole(auth, "ADMIN", "METADATA_UPDATER") ? metadataController.getStatus() : null,
                    isUserAdminOrImporter ? CoversSaverService.getStatus() : null
            );
        }
        return null;
    }

    public static String getStatusEndpoint() {
        return ROOT_ENDPOINT + STATUS_ENDPOINT;
    }
}

