package com.atsumeru.web.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class StringUtils {

    public static String join(String delimiter, Iterable<?> tokens) {
        Iterator<?> it = tokens.iterator();
        if (!it.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next());
        }
        return sb.toString();
    }

    public static String getFirstNotEmptyValue(String... values) {
        for (String value : values) {
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    public static boolean equals(String s1, String s2) {
        if (s1 == null) {
            s1 = "";
        }
        return s1.equals(s2);
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        if (s1 == null) {
            s1 = "";
        }
        return s1.equalsIgnoreCase(s2);
    }

    public static boolean startsWithIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().startsWith(second.toLowerCase());
    }

    public static boolean endsWithIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().endsWith(second.toLowerCase());
    }

    /**
     * Checks is StringBuilder null/empty
     *
     * @param str - input StringBuilder
     * @return boolean. true if null or empty, false if none of this
     */
    public static boolean isEmpty(StringBuilder str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks is string null/empty
     *
     * @param str - input string
     * @return boolean. true if null or empty, false if none of this
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Checks is string not null/not empty
     *
     * @param str - input string
     * @return boolean. true if not null and not empty, false if null or empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
     * Checks is StringBuilder not null/not empty
     *
     * @param str - input StringBuilder
     * @return boolean. true if not null and not empty, false if null or empty
     */
    public static boolean isNotEmpty(StringBuilder str) {
        return str != null && str.length() > 0;
    }

    /**
     * Checks is first {@link String} contains second {@link String} ignoring case
     *
     * @param first  - first {@link String}
     * @param second - second {@link String}
     * @return boolean. true if not first {@link String} contains second {@link String}
     */
    public static boolean containsIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().contains(second.toLowerCase());
    }

    public static String trimSquareBrackets(String str) {
        if (isNotEmpty(str)) {
            if (str.startsWith("[")) {
                str = str.substring(1);
            }
            if (str.endsWith("]")) {
                str = str.substring(0, str.length() - 1);
            }

            str = str.trim();
        }

        return str;
    }

    /**
     * Calculates MD5 for input string
     *
     * @param str - input string
     * @return - MD5 hash string
     */
    public static String md5Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NullPointerException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
