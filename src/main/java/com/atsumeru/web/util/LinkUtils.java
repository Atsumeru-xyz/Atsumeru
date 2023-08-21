package com.atsumeru.web.util;

import com.atsumeru.web.helper.HashHelper;

public class LinkUtils {

    /**
     * https://subdomain.domain.com/path -> domain
     */
    public static String getHostName(String link) {
        String host = HashHelper.getHost(link);
        if (host == null) {
            return "";
        }
        int levelTop = host.lastIndexOf(46);
        if (levelTop >= 0) {
            host = host.substring(0, levelTop);
        }
        if (host.contains(".")) {
            host = host.substring(host.lastIndexOf(".") + 1);
        }
        return host;
    }


    public static String getPath(String url) {
        if (url.startsWith("/")) {
            return url;
        }
        int i1 = url.indexOf("://");
        if (i1 < 0) {
            return null;
        }
        i1 += 3;
        i1 = url.indexOf(47, i1);
        if (i1 < 0) {
            return null;
        }
        int i2 = url.lastIndexOf(63);
        final int i3 = url.lastIndexOf(35);
        if (i2 >= 0 && i3 >= 0) {
            i2 = Math.min(i2, i3);
        } else if (i3 >= 0) {
            i2 = i3;
        } else if (i2 < 0) {
            i2 = url.length();
        }
        return url.substring(i1, i2).replace("//", "/");
    }
}
