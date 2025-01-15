package xyz.atsumeru.web.controller.rest.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.atsumeru.web.controller.rest.importer.ImporterApiController;
import xyz.atsumeru.web.controller.rest.metadata.MetadataApiController;
import xyz.atsumeru.web.exception.MetadataUpdateActiveException;
import xyz.atsumeru.web.model.service.ServicesStatus;
import xyz.atsumeru.web.security.service.UsersDetailsService;
import xyz.atsumeru.web.service.CoversSaverService;
import xyz.atsumeru.web.service.MetadataUpdateService;

@RestController
@RequestMapping(ServicesApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('IMPORTER', 'METADATA_UPDATER')")
public class ServicesApiController {
    protected static final String ROOT_ENDPOINT = "/api/v1/services";
    private static final String STATUS_ENDPOINT = "/status";

    private final ImporterApiController importerController;
    private final MetadataApiController metadataController;

    public ServicesApiController(ImporterApiController importerController, MetadataApiController metadataController) {
        this.importerController = importerController;
        this.metadataController = metadataController;
    }

    public static void checkIsBlockingServicesRunning(boolean isServiceStatusRequest) {
        if (MetadataUpdateService.isUpdateActive() && !isServiceStatusRequest) {
            throw new MetadataUpdateActiveException();
        }
    }

    @GetMapping(STATUS_ENDPOINT)
    public ServicesStatus getStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isUserAdminOrImporter = UsersDetailsService.isUserInRole(auth, "ADMIN", "IMPORTER");
            return new ServicesStatus(
                    isUserAdminOrImporter ? importerController.getStatus() : null,
                    UsersDetailsService.isUserInRole(auth, "ADMIN", "METADATA_UPDATER") ? metadataController.getStatus() : null,
                    isUserAdminOrImporter ? CoversSaverService.getStatus() : null
            );
        }
        return null;
    }

    public static String getStatusEndpoint() {
        return ROOT_ENDPOINT + STATUS_ENDPOINT;
    }
}

