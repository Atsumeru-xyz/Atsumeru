package com.atsumeru.web.util.comparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalStringComparator implements Comparator<String> {
    private static final Pattern splitPattern = Pattern.compile("[0-9０-９]+|\\.|\\s");

    @Override
    public int compare(String str1, String str2) {
        return compareStrings(str1, str2);
    }

    public static int compareStrings(String str1, String str2) {
        Iterator<String> i1 = splitStringPreserveDelimiter(str1).iterator();
        Iterator<String> i2 = splitStringPreserveDelimiter(str2).iterator();
        while (true) {
            //Til here all is equal.
            if (!i1.hasNext() && !i2.hasNext()) {
                return 0;
            }
            //first has no more parts -> comes first
            if (!i1.hasNext()) {
                return -1;
            }
            //first has more parts than i2 -> comes after
            if (!i2.hasNext()) {
                return 1;
            }

            String data1 = i1.next();
            String data2 = i2.next();
            int result;
            try {
                //If both datas are numbers, then compare numbers
                result = Long.compare(Long.parseLong(data1), Long.parseLong(data2));
                //If numbers are equal than longer comes first
                if (result == 0) {
                    result = -Integer.compare(data1.length(), data2.length());
                }
            } catch (NumberFormatException ex) {
                //compare text case insensitive
                result = data1.compareToIgnoreCase(data2);
            }

            if (result != 0) {
                return result;
            }
        }
    }

    private static List<String> splitStringPreserveDelimiter(String str) {
        Matcher matcher = splitPattern.matcher(str);
        List<String> list = new ArrayList<>();
        int pos = 0;
        while (matcher.find()) {
            list.add(str.substring(pos, matcher.start()));
            list.add(matcher.group());
            pos = matcher.end();
        }
        list.add(str.substring(pos));
        return list;
    }
}