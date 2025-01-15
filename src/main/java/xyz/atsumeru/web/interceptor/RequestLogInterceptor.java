package xyz.atsumeru.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.atsumeru.web.controller.rest.ServerApiController;
import xyz.atsumeru.web.controller.rest.importer.ImporterApiController;
import xyz.atsumeru.web.controller.rest.metadata.MetadataApiController;
import xyz.atsumeru.web.controller.rest.service.ServicesApiController;
import xyz.atsumeru.web.controller.rest.uploader.UploaderApiController;
import xyz.atsumeru.web.helper.ServerHelper;
import xyz.atsumeru.web.logger.FileLogger;
import xyz.atsumeru.web.manager.Settings;
import xyz.atsumeru.web.manager.Workspace;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.TypeUtils;

import java.io.File;
import java.security.Principal;
import java.util.Date;
import java.util.Optional;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogInterceptor.class.getSimpleName());
    private static java.util.logging.Logger fileLogger;

    private boolean isDisableRequestLoggingIntoConsole;

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    public void onSettingsUpdate() {
        fileLogger = FileLogger.createLogger("RequestLog", new File(Workspace.LOGS_DIR, "requests.log"));
        isDisableRequestLoggingIntoConsole = Settings.isDisableRequestLoggingIntoConsole();
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String requestedUrl = ServerHelper.getRequestedRelativeURL(request);
        boolean isServiceStatusRequest = isServiceStatusRequest(requestedUrl);
        if (isLoggableRequest(requestedUrl) && !isServiceStatusRequest) {
            boolean doNotTrack = Optional.ofNullable(request.getHeader("DNT"))
                    .map(value -> TypeUtils.getIntDef(value, 0))
                    .map(value -> value == 1)
                    .orElse(false);

            String ipAddress = !doNotTrack
                    ? Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                            .filter(StringUtils::isNotEmpty)
                            .orElseGet(request::getRemoteAddr)
                    : "Private";

            String userName = Optional.ofNullable(request.getUserPrincipal())
                    .map(Principal::getName)
                    .orElse("Unknown");

            String log = "[" + ipAddress + "@" + userName + "] Requested " + requestedUrl;
            if (!isPingEndpoint(requestedUrl) && !isDisableRequestLoggingIntoConsole) {
                logger.info(log);
            }
            fileLogger.info("[" + new Date() + "] " + log);
        }

        ServicesApiController.checkIsBlockingServicesRunning(
                isServiceStatusRequest || isUploadServiceRequest(requestedUrl)
        );
        return true;
    }

    private boolean isServiceStatusRequest(String requestedUrl) {
        return requestedUrl.equals(ImporterApiController.getStatusEndpoint())
                || requestedUrl.equals(MetadataApiController.getStatusEndpoint())
                || requestedUrl.equals(ServicesApiController.getStatusEndpoint())
                || requestedUrl.contains(UploaderApiController.ROOT_ENDPOINT)
                || requestedUrl.startsWith("/error");
    }

    private boolean isPingEndpoint(String requestedUrl) {
        return requestedUrl.equals(ServerApiController.getPingEndpoint());
    }

    private boolean isUploadServiceRequest(String requestedUrl) {
        return requestedUrl.contains(UploaderApiController.ROOT_ENDPOINT);
    }

    private boolean isLoggableRequest(String requestedUrl) {
        return !requestedUrl.startsWith("/error");
    }
}
