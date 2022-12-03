package com.atsumeru.web.service;

import com.atsumeru.web.component.AppInfo;
import com.atsumeru.web.helper.ServerHelper;
import com.atsumeru.web.manager.Settings;
import com.atsumeru.web.util.NotEmptyString;
import de.mannodermaus.rxbonjour.BonjourBroadcastConfig;
import de.mannodermaus.rxbonjour.RxBonjour;
import de.mannodermaus.rxbonjour.drivers.jmdns.JmDNSDriver;
import de.mannodermaus.rxbonjour.platforms.desktop.DesktopPlatform;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BonjourService {
    private static final Logger logger = LoggerFactory.getLogger(BonjourService.class.getSimpleName());

    @Autowired
    private Environment environment;
    @Autowired
    private AppInfo appInfo;

    private Disposable disposable;

    public void restartService() {
        if (Settings.isDisableBonjourService()) {
            stopService();
        } else if (!isServiceStarted()) {
            startService();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(9)
    public void startService() {
        if (Settings.isDisableBonjourService()) {
            logger.warn("Bonjour service disabled");
            return;
        }

        String serviceName = "Atsumeru " + getFormattedAppInfo();

        logger.info("Starting Bonjour service with name " + serviceName + "...");
        RxBonjour rxBonjour = new RxBonjour.Builder()
                .platform(DesktopPlatform.create())
                .driver(JmDNSDriver.create())
                .create();

        Map<String, String> map = new HashMap<>();
        map.put("local.address", ServerHelper.getLocalAddress());
        map.put("local.host_name", ServerHelper.getLocalHostName());
        map.put("remote.address", ServerHelper.getRemoteAddress());
        map.put("remote.host_name", ServerHelper.getRemoteHostName());
        map.put("port", ServerHelper.getPort(environment));
        map.put("version", getAppInfo());

        logger.info("Bonjour variables: " + map);

        BonjourBroadcastConfig broadcastConfig = new BonjourBroadcastConfig(
                "_atsumeru._tcp",
                serviceName,
                null,
                Integer.parseInt(ServerHelper.getPort(environment)),
                map);

        disposable = rxBonjour.newBroadcast(broadcastConfig)
                .subscribeOn(Schedulers.io())
                .doOnError(Throwable::printStackTrace)
                .subscribe();

        logger.info("Bonjour service started");
    }

    public void stopService() {
        if (isServiceStarted()) {
            disposable.dispose();
            disposable = null;
            logger.info("Bonjour service stopped");
        }
    }

    public boolean isServiceStarted() {
        return disposable != null;
    }

    public String getAppInfo() {
        return NotEmptyString.ofNullable(appInfo.getManifestInfo()).orElse("debug");
    }

    public String getFormattedAppInfo() {
        return String.format("(v%s)", getAppInfo());
    }
}
