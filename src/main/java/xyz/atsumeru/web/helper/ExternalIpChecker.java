package xyz.atsumeru.web.helper;

import xyz.atsumeru.web.util.FileUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class ExternalIpChecker {
    private static final String AMAZON_IP_CHECKER_URL = "http://checkip.amazonaws.com";

    public static String getExternalIp() {
        BufferedReader in = null;
        try {
            URL url = new URL(AMAZON_IP_CHECKER_URL);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            return in.readLine();
        } catch (Exception ex) {
            return null;
        } finally {
            FileUtils.closeLoudly(in);
        }
    }
}