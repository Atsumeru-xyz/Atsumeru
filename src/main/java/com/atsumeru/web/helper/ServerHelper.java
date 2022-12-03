package com.atsumeru.web.helper;

import com.atsumeru.web.util.NotEmptyString;
import org.springframework.core.env.Environment;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Predicate;

public class ServerHelper {
    private static final String DEFAULT_PORT = "8080";
    private static final Predicate<InetAddress> NOT_SUPPORTED_ADDRESS =
            inetAddress -> inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isMulticastAddress();

    public static String getPort(Environment environment) {
        return NotEmptyString
                .ofNullable(environment.getProperty("server.port"))
                .orElse(DEFAULT_PORT);
    }

    private static InetAddress getInetAddress() throws SocketException {
        Enumeration<NetworkInterface> iterNetwork;
        Enumeration<InetAddress> iterAddress;
        NetworkInterface network;
        InetAddress address;

        iterNetwork = NetworkInterface.getNetworkInterfaces();
        while (iterNetwork.hasMoreElements()) {
            network = iterNetwork.nextElement();

            if (!network.isUp()) {
                continue;
            }

            if (network.isLoopback()) {
                continue;
            }

            iterAddress = network.getInetAddresses();
            while (iterAddress.hasMoreElements()) {
                address = iterAddress.nextElement();

                if (NOT_SUPPORTED_ADDRESS.test(address)) {
                    continue;
                }

                return address;
            }
        }
        return null;
    }

    public static String getLocalAddress() {
        try {
            return Optional.ofNullable(getInetAddress())
                    .map(InetAddress::getHostAddress)
                    .orElse(null);
        } catch (SocketException ex) {
            return null;
        }
    }

    public static String getLocalHostName() {
        try {
            return Optional.ofNullable(getInetAddress())
                    .map(InetAddress::getHostName)
                    .orElse(null);
        } catch (SocketException ex) {
            return null;
        }
    }

    public static String getRemoteAddress() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    public static String getRemoteHostName() {
        return InetAddress.getLoopbackAddress().getHostName();
    }

    public static String getExternalAddress() {
        return ExternalIpChecker.getExternalIp();
    }

    public static String getRequestedRelativeURL(HttpServletRequest req) {
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();   // d=789

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (queryString != null) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }

    public static String getRequestedURLPath(HttpServletRequest req) {
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }
        return url.toString();
    }
}
