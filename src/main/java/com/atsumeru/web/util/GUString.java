package com.atsumeru.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GUString {

    public static List<String> splitEqually(String text, int size) {
        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

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

    /**
     * Decodes a {@code application/x-www-form-urlencoded} string using a specific
     * encoding scheme.
     * @param url the {@code String} to decode
     * @return the newly decoded {@code String}
     */
    public static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return url;
        } catch (NullPointerException ex2) {
            return url;
        }
    }

    /**
     * Translates a string into {@code application/x-www-form-urlencoded}
     * format using a specific encoding scheme. This method uses the
     * supplied encoding scheme to obtain the bytes for unsafe
     * characters.
     * @param url the {@code String} to encode
     * @return the translated {@code String}.
     */
    public static String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return url;
        }
    }

    public static boolean isEquals(String compareTo, String comparableStr) {
        return compareTo != null && compareTo.equals(comparableStr);
    }

    public static boolean equals(String s1, String s2) {
        if (s1 == null)
            s1 = "";
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
     * @param str - input StringBuilder
     * @return boolean. true if null or empty, false if none of this
     */
    public static boolean isEmpty(StringBuilder str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks is string null/empty
     * @param str - input string
     * @return boolean. true if null or empty, false if none of this
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Checks is string not null/not empty
     * @param str - input string
     * @return boolean. true if not null and not empty, false if null or empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    /**
     * Checks is StringBuilder not null/not empty
     * @param str - input StringBuilder
     * @return boolean. true if not null and not empty, false if null or empty
     */
    public static boolean isNotEmpty(StringBuilder str) {
        return str != null && str.length() > 0;
    }

    /**
     * Checks is first {@link String} contains second {@link String} ignoring case
     * @param first - first {@link String}
     * @param second - second {@link String}
     * @return boolean. true if not first {@link String} contains second {@link String}
     */
    public static boolean containsIgnoreCase(String first, String second) {
        return isNotEmpty(first) && isNotEmpty(second) && first.toLowerCase().contains(second.toLowerCase());
    }

    /**
     * Remove braces from string (with braces content) and trimmed
     * @param str - input string
     * @param removeAll - boolean flag. If true, all founded blocks with braces will be removed
     * @param fixDoubleSpaces - boolean flag. If true, it will replace double spaces with one space
     * @return string without braces and trimmed
     */
    public static String removeBraces(String str, boolean removeAll, boolean fixDoubleSpaces) {
        int indexBraceLeft = str.indexOf("(");
        if (indexBraceLeft < 0) {
            return str;
        }
        int indexBraceRight = str.indexOf(")", indexBraceLeft);
        if (indexBraceRight < 0) {
            return str;
        }
        if (removeAll) {
            str = str.substring(0, indexBraceLeft) + removeBraces(str.substring(indexBraceRight + 1), true, fixDoubleSpaces);
            return fixDoubleSpaces ? str.replaceAll(" {2}", " ").trim() : str.trim();
        }
        str = str.substring(0, indexBraceLeft) + str.substring(indexBraceRight + 1);
        return fixDoubleSpaces ? str.replaceAll(" {2}", " ").trim() : str.trim();
    }

    /**
     * Remove square brackets from string (with brackets content)
     * @param str - input string
     * @param removeAll - boolean flag. If true, all founded blocks with square brackets will be removed
     * @param fixDoubleSpaces - boolean flag. If true, it will replace double spaces with one space
     * @return string without square brackets
     */
    public static String removeSquareBrackets(String str, boolean removeAll, boolean fixDoubleSpaces) {
        int indexBracketLeft = str.indexOf("[");
        if (indexBracketLeft < 0) {
            return str;
        }
        int indexBracketRight = str.indexOf("]", indexBracketLeft);
        if (indexBracketRight < 0) {
            return str;
        }
        if (removeAll) {
            str = str.substring(0, indexBracketLeft) + removeSquareBrackets(str.substring(indexBracketRight + 1), true, fixDoubleSpaces);
            return fixDoubleSpaces ? str.replaceAll(" {2}", " ").trim() : str.trim();
        }
        str = str.substring(0, indexBracketLeft) + str.substring(indexBracketRight + 1);
        return fixDoubleSpaces ? str.replaceAll(" {2}", " ") : str;
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
     * Removes all html tags from input string leaving only readable text
     * @param htmlStr - input html string
     * @return string without html tags
     */
    public static String fromHtmlToString(String htmlStr) {
        if (isEmpty(htmlStr)) {
            return "";
        }
        htmlStr = htmlStr.replace("<br />", "\n");
        StringBuilder str = new StringBuilder();
        int indexStart = 0;
        int indexEnd = htmlStr.indexOf("<", indexStart);
        if (indexEnd >= 0) {
            str.append(htmlStr, indexStart, indexEnd);
            while (indexStart >= 0 && indexEnd >= 0) {
                indexStart = htmlStr.indexOf(">", indexEnd);
                indexEnd = htmlStr.indexOf("<", indexStart);
                boolean flBreak = false;
                if (indexEnd < 0) {
                    indexEnd = htmlStr.length();
                    flBreak = true;
                }
                str.append(htmlStr, indexStart + 1, indexEnd);
                if (flBreak) {
                    break;
                }
            }
        } else {
            str.append(htmlStr);
        }
        return str.toString().trim();
    }

    /**
     * Calculates MD5 for input string
     * @param str - input string
     * @return - MD5 hash string
     */
    public static String md5Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; ++i) {
                final String hex = Integer.toHexString(0xFF & hash[i]);
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

    public static String toUppercaseFirst(String string) {
        if (string != null && string.length() != 0) {
            return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
        }
        return string;
    }

    public static String capitalizeFully(String str) {
        return capitalizeFully(str, null);
    }

    public static String capitalizeFully(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        str = str.toLowerCase();
        return capitalize(str, delimiters, false, true);
    }

    public static String capitalize(String str) {
        return capitalize(str, null, false, true);
    }

    public static String capitalize(String str, boolean isLowercaseBeforeCapitalize, boolean capitalizeAllBetweenDelimiters) {
        return capitalize(str, null, isLowercaseBeforeCapitalize, capitalizeAllBetweenDelimiters);
    }

    public static String capitalize(String str, char[] delimiters, boolean isLowercaseBeforeCapitalize, boolean capitalizeAllBetweenDelimiters) {
        if (isLowercaseBeforeCapitalize) {
            str = str.toLowerCase();
        }

        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                capitalizeNext = capitalizeAllBetweenDelimiters;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }

    public static String getFirstNotEmptyValue(String... values) {
        for (String value : values) {
            if (GUString.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }
}
