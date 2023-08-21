package com.atsumeru.web.helper;

import com.atsumeru.web.util.StringUtils;

public class HashHelper {
    public static final String VAR_SCHEME = "scheme";

    public static String getMHash2(String hashTag, String link) {
        String hash = getUriHash2(hashTag, link);
        return hashTag != null ? hashTag + hash : hash;
    }

    public static String getHost(String link) {
        final int scheme = link.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        final int start = scheme + 3;
        int end = link.indexOf(47, start);
        if (end < 0) {
            end = link.length();
        }
        return link.substring(start, end);
    }

    private static String getPath(String link) {
        int q = link.indexOf("?");
        int h = link.indexOf("#");
        int end;
        if (q < 0 && h < 0) {
            end = link.length();
        } else if (q >= 0 && h >= 0) {
            end = Math.min(q, h);
        } else if (q >= 0) {
            end = q;
        } else {
            end = h;
        }
        int scheme = link.contains(VAR_SCHEME) ? link.indexOf("//") : link.indexOf("://");
        int start = 0;
        if (scheme >= 0) {
            start = link.indexOf(47, scheme + 3);
        }
        if (start < 0) {
            return null;
        }
        return link.substring(start, end);
    }

    private static String getUriHash2(String hashTag, String link) {
        String path = getPath(link);
        if (path == null) {
            return StringUtils.md5Hex(link);
        }
        path = path.replace("//", "/");
        return hashTag == null ? StringUtils.md5Hex(path) : StringUtils.md5Hex(hashTag + path);
    }
}