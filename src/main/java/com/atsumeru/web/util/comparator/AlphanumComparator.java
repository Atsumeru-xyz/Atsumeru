package com.atsumeru.web.util.comparator;

import java.util.Comparator;

public class AlphanumComparator<T> implements Comparator<T> {

    @Override
    public int compare(T obj1, T obj2) {
        return compareStrings(obj1.toString(), obj2.toString());
    }

    public static <T> int compareObjToString(T obj1, T obj2) {
        return compareStrings(obj1.toString(), obj2.toString());
    }

    public static int compareStrings(String string1, String string2) {
        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = string1.length();
        int s2Length = string2.length();
        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = getChunk(string1, s1Length, thisMarker);
            thisMarker += thisChunk.length();
            String thatChunk = getChunk(string2, s2Length, thatMarker);
            thatMarker += thatChunk.length();
            int result;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; ++i) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk);
            }
            if (result != 0) {
                return result;
            }
        }
        return s1Length - s2Length;
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static String getChunk(String string, int stringLength, int marker) {
        final StringBuilder chunk = new StringBuilder();
        char c = string.charAt(marker);
        chunk.append(c);
        ++marker;
        if (isDigit(c)) {
            while (marker < stringLength) {
                c = string.charAt(marker);
                if (!isDigit(c)) {
                    break;
                }
                chunk.append(c);
                ++marker;
            }
        } else {
            while (marker < stringLength) {
                c = string.charAt(marker);
                if (isDigit(c)) {
                    break;
                }
                chunk.append(c);
                ++marker;
            }
        }
        return chunk.toString();
    }
}