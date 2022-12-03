package com.atsumeru.web.util;

import com.atsumeru.web.helper.HashHelper;

import java.net.URLDecoder;

public class GULinks {

    /**
     * https://online.anidub.com/test -> online.anidub.com
     * @param url
     * @return
     */
    public static String getUrlHostName(String url) {
        int i1 = url.indexOf("://");
        if (i1 < 0) {
            return "";
        }
        i1 += 3;
        final int i2 = url.indexOf("/", i1);
        if (i2 < 0) {
            return url.substring(i1);
        }
        return url.substring(i1, i2);
//        return url.substring(0, i2);
    }

    /**
     * https://online.anidub.com/test -> anidub.com
     * @param link
     * @return
     */
    public static String getBaseUrl(String link) {
        return GUString.isNotEmpty(link)
                ? getHostName(link) + "." + getHostTopName(link)
                : link;
    }

    /**
     * https://online.anidub.com/test -> anidub
     * @param link
     * @return
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

    /**
     * https://online.anidub.com/test -> com
     * @param link
     * @return
     */
    public static String getHostTopName(String link) {
        String topName = HashHelper.getHost(link);
        if (topName == null) {
            return "";
        }
        int levelTop = topName.lastIndexOf(46);
        if (levelTop >= 0) {
            topName = topName.substring(levelTop + 1);
        }
        if (topName.contains(":")) {
            topName = topName.substring(0, topName.lastIndexOf(":"));
        }
        return topName;
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
        }
        else if (i3 >= 0) {
            i2 = i3;
        }
        else if (i2 < 0) {
            i2 = url.length();
        }
        return url.substring(i1, i2).replace("//", "/");
    }

    public static String decodeUrlPath(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (Exception e) {
            //e.printStackTrace();
            return url;
        }
    }

    public static String getAbsoluteTrueUrl(String baseLink, String link) {
        if (link.contains("://") || link.startsWith("javascript")) {
            return link;
        }
        if (link.startsWith("//")) {
            return getUrlScheme(baseLink) + link;
        }
        if (link.startsWith("/")) {
            return delPathSlash(getUrlHost(baseLink)) + link;
        }
        if (link.startsWith("?")) {
            final int i = baseLink.lastIndexOf(63);
            if (i >= 0) {
                baseLink = baseLink.substring(0, i);
            }
            return baseLink + link;
        }
        if (link.startsWith("#")) {
            final int i = baseLink.lastIndexOf(35);
            if (i >= 0) {
                baseLink = baseLink.substring(0, i);
            }
            return baseLink + link;
        }
        if (link.length() == 0) {
            return baseLink;
        }
        return addPathSlash(getDirName(baseLink)) + link;
    }

    /**
     * https://anidub.com/test -> https:
     * @param url - input url
     * @return - scheme. ex: https:
     */
    public static String getUrlScheme(String url) {
        final int i1 = url.indexOf("//");
        if (i1 < 0) {
            return "";
        }
        return url.substring(0, i1);
    }

    public static String addPathSlash(final String dir) {
        String result = dir.trim();
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    public static String delPathSlash(String dir) {
        if (dir == null) {
            return null;
        }

        String result = dir.trim();
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * https://online.anidub.com/test -> https://online.anidub.com
     * @param url
     * @return
     */
    public static String getUrlHost(String url) {
        int i1 = url.indexOf("://");
        if (i1 < 0) {
            return null;
        }
        i1 += 3;
        final int i2 = url.indexOf("/", i1);
        if (i2 < 0) {
            return url;
        }
        return url.substring(0, i2);
    }

    public static String getDirName(final String link) {
        if (link.endsWith("/")) {
            return link;
        }
        return link.substring(0, link.lastIndexOf("/") + 1);
    }
}
