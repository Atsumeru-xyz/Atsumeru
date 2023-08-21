package com.atsumeru.web.util;

import java.util.*;
import java.util.stream.Collectors;

public class ArrayUtils {

    public static <T> boolean isInSet(Set<T> set, T item) {
        return isNotEmpty(set) && set.contains(item);
    }

    public static List<String> splitString(String str) {
        return splitString(str, ",");
    }

    public static List<String> splitString(String str, String regex) {
        try {
            return Arrays.stream(str.split(regex))
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        } catch (NullPointerException ex) {
            return new ArrayList<>();
        }
    }

    public static String safeGetString(List<String> list, int index, String def) {
        try {
            return list.get(index);
        } catch (Exception ex) {
            return def;
        }
    }

    public static void fillSetWithStringAsArray(Set<String> set, String strArray) {
        if (StringUtils.isNotEmpty(strArray)) {
            fillSetWithList(set, splitString(strArray, ","));
        }
    }

    public static void fillSetWithList(Set<String> set, List<String> strings) {
        for (String str : strings) {
            if (StringUtils.isEmpty(str)) {
                continue;
            }
            set.add(str.trim());
        }
    }

    public static <T> List<T> getNotNullList(List<T> list) {
        return isNotEmpty(list) ? list : new ArrayList<>();
    }

    /**
     * Check is collection empty
     *
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - true if empty / false if not
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Object collectionMapArray, Integer... lengthArr) {
        int length = lengthArr != null && lengthArr.length > 0 ? lengthArr[0] : 1;
        if (collectionMapArray == null) {
            return true;
        } else if (collectionMapArray instanceof Collection) {
            return ((Collection) collectionMapArray).size() < length;
        } else if (collectionMapArray instanceof Map) {
            return ((Map) collectionMapArray).size() < length;
        } else if (collectionMapArray instanceof Object[]) {
            return ((Object[]) collectionMapArray).length < length || ((Object[]) collectionMapArray)[length - 1] == null;
        } else return true;
    }

    /**
     * Acts like {@link #isEmpty(Object, Integer...) isEmpty} method but reverse
     *
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - false if empty / true if not
     */
    public static boolean isNotEmpty(Object collectionMapArray) {
        return !isEmpty(collectionMapArray);
    }

    public static boolean isNotNull(Object collectionMapArray) {
        return collectionMapArray != null;
    }

    public static <T> T getLastItem(List<T> collection) {
        if (isNotEmpty(collection)) {
            return collection.get(collection.size() - 1);
        }
        return null;
    }
}