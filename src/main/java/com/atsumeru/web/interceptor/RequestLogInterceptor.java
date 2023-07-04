package com.atsumeru.web.interceptor;

import com.atsumeru.web.controller.rest.importer.ImporterApiController;
import com.atsumeru.web.controller.rest.uploader.UploaderApiController;
import com.atsumeru.web.controller.rest.ServerApiController;
import com.atsumeru.web.controller.rest.metadata.MetadataApiController;
import com.atsumeru.web.controller.rest.service.ServicesApiController;
import com.atsumeru.web.helper.ServerHelper;
import com.atsumeru.web.logger.FileLogger;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.util.WorkspaceUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Date;
import java.util.Optional;

@Component
public class RequestLogInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogInterceptor.class.getSimpleName());
    private static final java.util.logging.Logger fileLogger;

    private boolean isDisableRequestLoggingIntoConsole;

    @EventListener(ApplicationReadyEvent.class)
    @Order(9)
    public void onSettingsUpdate() {
        isDisableRequestLoggingIntoConsole = Settings.isDisableRequestLoggingIntoConsole();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String userName = Optional.ofNullable(request.getUserPrincipal())
                .map(Principal::getName)
                .orElse("Unknown");

        String requestedUrl = ServerHelper.getRequestedRelativeURL(request);
        boolean isServiceStatusRequest = isServiceStatusRequest(requestedUrl);
        if (isLoggableRequest(requestedUrl) && !isServiceStatusRequest) {
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

    static {
        fileLogger = FileLogger.createLogger("RequestLog", WorkspaceUtils.LOGS_DIR + "requests.log");
    }
}
