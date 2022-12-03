package com.atsumeru.web.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class GUArray {

    public static List<String> splitString(String str) {
        return splitString(str, ",");
    }

    public static List<String> splitString(String str, String regex) {
        try {
            return Arrays.stream(str.split(regex))
                    .filter(GUString::isNotEmpty)
                    .collect(Collectors.toList());
        } catch (NullPointerException ex) {
            return new ArrayList<>();
        }
    }

    public static String safeGetString(List<String> list, int index) {
        return safeGetString(list, index, null);
    }

    public static String safeGetString(List<String> list, int index, String def) {
        try {
            return list.get(index);
        } catch (Exception ex) {
            return def;
        }
    }

    public static void fillSetWithStringAsArray(Set<String> set, String strArray) {
        if (GUString.isNotEmpty(strArray)) {
            fillSetWithList(set, splitString(strArray, ","));
        }
    }

    public static void fillSetWithList(Set<String> set, List<String> strings) {
        for (String str : strings) {
            if (GUString.isEmpty(str)) {
                continue;
            }
            set.add(str.trim());
        }
    }

    public static void removeDuplicates(ArrayList<String> list) {
        Set<String> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);
    }

    public static boolean addStringIntoArrayIfAbsent(String value, ArrayList<String> dest, int maxValueLength, boolean isCleanAndTrim, boolean lowercase) {
        return addStringIntoArrayIfAbsent(value, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, isCleanAndTrim, lowercase);
    }

    public static boolean addStringIntoArrayIfAbsent(String value, ArrayList<String> dest, int maxValueLength,
                                                     int maxArraySize, boolean isCleanAndTrim, boolean lowercase) {
        if (GUString.isEmpty(value) || value.length() > maxValueLength || dest.size() > maxArraySize || dest.contains(value)) {
            return false;
        }
        if (isCleanAndTrim) {
            value = GUHtml.unescape(GUString.fromHtmlToString(value)).trim();
        }
        if (lowercase) {
            value = value.toLowerCase();
        }
        dest.add(value);
        return true;
    }

    public static boolean mergeArraysWithoutDuplicates(ArrayList<String> values, ArrayList<String> dest, boolean isCleanAndTrim, boolean lowercase) {
        return mergeArraysWithoutDuplicates(values, dest, Integer.MAX_VALUE, Integer.MAX_VALUE, isCleanAndTrim, lowercase);
    }

    public static boolean mergeArraysWithoutDuplicates(ArrayList<String> values, ArrayList<String> dest, int maxValueLength,
                                                       int maxArraySize, boolean isCleanAndTrim, boolean lowercase) {
        if (GUArray.isEmpty(values)) {
            return false;
        }
        boolean merged = false;
        for (String value : values) {
            if (isCleanAndTrim) {
                value = GUHtml.unescape(GUString.fromHtmlToString(value)).trim();
            }
            if (lowercase) {
                value = value.toLowerCase();
            }
            if (addStringIntoArrayIfAbsent(value, dest, maxValueLength, maxArraySize, false, false)) {
                merged = true;
            }
        }
        return merged;
    }

    public static <T> List<T> getNotNullList(List<T> list) {
        return isNotEmpty(list) ? list : new ArrayList<>();
    }

    /**
     * Add all items from list into collection
     * @param adapter - collection where need to add items
     * @param list - list, from which need to get items
     * @param <T> - collection with new items
     */
    public static <T> void addAll(Collection<T> adapter, List<T> list) {
        for (T o : list) {
            adapter.add(o);
        }
    }

    /** Check is collection empty
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - true if empty / false if not
     */
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

    /** Acts like {@link #isEmpty(Object, Integer...) isEmpty} method but reverse
     * @param collectionMapArray Collection, Map or Array
     * @return boolean - false if empty / true if not
     */
    public static boolean isNotEmpty(Object collectionMapArray) {
        return !isEmpty(collectionMapArray);
    }

    public static boolean isNotNull(Object collectionMapArray) {
        return collectionMapArray != null;
    }

    /** Returns array if not empty or null
     * @param list List for check
     * @return List<String> if not empty or null
     */
    public static List<String> getArrayOrNull(List<String> list) {
        if (list.size() == 0) {
            return null;
        }
        return list;
    }

    public static <T> T findItemFromSortList(T item, List<T> list, Comparator comparator) {
        int var3 = Collections.binarySearch(list, item, comparator);
        if (var3 < 0) {
            item = null;
        } else {
            item = list.get(var3);
        }

        return item;
    }

    public static <E> E[] toArray(Collection<E> list) {
        if (isEmpty(list))
            return null;
        return list.toArray((E[]) Array.newInstance(list.iterator().next().getClass(), list.size()));
    }

    public static <E> ArrayList<E> toArrayList(E[] split) {
        ArrayList<E> list = new ArrayList<>();
        if (!GUArray.isEmpty(split))
            for (E e : split) {
                list.add(e);
            }
        return list;
    }

    /**
     * @param <E>
     * @param output         no dupplicates
     * @param input          may be not sorted
     * @param comparator
     * @param replaceWithNew
     */
    public static <E> ArrayList<E> addAllUniq(ArrayList<E> output, ArrayList<E> input, Comparator<E> comparator, boolean replaceWithNew) {
        ArrayList<E> inputCopy = new ArrayList<>(input);
        Collections.sort(inputCopy, comparator);
        for (E e : output) {
            int index = Collections.binarySearch(inputCopy, e, comparator);
            if (index < 0)
                input.add(e);
            else if (replaceWithNew) {
                int place = input.indexOf(inputCopy.get(index));
                if (place > -1)
                    input.set(place, e);
            }
        }
        return input;
    }

    public static String toUppercaseFirstFromList(List<String> list) {
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            String stringFromList = list.get(i);
            str.append(GUString.toUppercaseFirst(stringFromList));
            if (i + 1 != list.size()) {
                str.append(", ");
            }
        }
        return str.toString();
    }

    public static ArrayList<String> toUppercaseFirstInList(List<String> list) {
        ArrayList<String> arr = new ArrayList<>();

        for (String stringFromList : list) {
            arr.add(GUString.toUppercaseFirst(stringFromList));
        }
        return arr;
    }

    public static ArrayList<String> toUppercaseAllInList(List<String> list) {
        ArrayList<String> arr = new ArrayList<>();

        for (String stringFromList : list) {
            if (!stringFromList.isEmpty()) {
                StringBuilder str = new StringBuilder(stringFromList);
                int i = 0;
                do {
                    str.replace(i, i + 1, str.substring(i, i + 1).toUpperCase());
                    i = str.indexOf(" ", i) + 1;
                } while (i > 0 && i < str.length());
                arr.add(str.toString());
            }
        }
        return arr;
    }

    public static List<String> capitalizeFully(List<String> list) {
        ArrayList<String> arr = new ArrayList<>();

        for (String str : list) {
            arr.add(GUString.capitalizeFully(str));
        }

        return arr;
    }

    public static <T> T getLastItem(List<T> collection) {
        if (isNotEmpty(collection)) {
            return collection.get(collection.size() - 1);
        }
        return null;
    }
}