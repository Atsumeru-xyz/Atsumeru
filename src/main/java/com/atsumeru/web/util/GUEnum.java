package com.atsumeru.web.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GUEnum {
    private static final String ENUM_NAME_PATTERN = "^.|.$";

    /**
     * Converts human readable enum name to allowed enum name replacing spaces to _
     * @param name {@link String} human readable enum name
     * @return {@link String} allowed enum name
     */
    public static String convertHumanizedToEnumName(String name) {
        return NotEmptyString.ofNullable(name)
                .apply(input -> input.replace(" ", "_").toUpperCase())
                .orElse(name);
    }

    /**
     * Returns {@link String[]} of all names for provided Enum class
     * @param e enum class
     * @return {@link String[]} of all enum names
     */
    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll(ENUM_NAME_PATTERN, "").split(", ");
    }

    /**
     * Returns lowercased {@link String[]} of all names for provided Enum class
     * @param e enum class
     * @return lowercased {@link String[]} of all enum names
     */
    public static String[] getNamesLowerCased(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll(ENUM_NAME_PATTERN, "").toLowerCase().split(", ");
    }

    /**
     * Returns lowercased and capitalized {@link String} name for provided Enum value
     * @param e enum value
     * @return lowercased and capitalized {@link String} name
     */
    public static <E extends Enum<E>> String getNameCapitalized(E e, boolean replaceUnderscoresToSpaces) {
        String name = e.name();
        if (replaceUnderscoresToSpaces) {
            name = name.replace("_", " ");
        }
        return GUString.capitalize(name, true, false);
    }

    /**
     * Returns singleton {@link List} of lowercased and capitalized {@link String} name for provided Enum value
     * @param e enum value
     * @return singleton {@link List} of lowercased and capitalized {@link String} name
     */
    public static <E extends Enum<E>> List<String> getNameCapitalizedAsList(E e, boolean replaceUnderscoresToSpaces) {
        return Collections.singletonList(getNameCapitalized(e, replaceUnderscoresToSpaces));
    }

    /**
     * Returns Enum by id
     * @param e enum
     * @param n enum id
     * @param <E> type of enum class
     * @return enum that match provided id
     */
    public static <E extends Enum<E>> E get(E e, Integer n) {
        Object t = null;
        try {
            t = e.getDeclaringClass().getMethod("values", (Class[]) null).invoke(e, (Object[]) null);
        } catch (Exception ignored) {
        }
        for (E eItem : ((E[]) t)) {
            if (eItem.ordinal() == n)
                return eItem;
        }
        return e;
    }

    /**
     * Returns Enum by id
     * @param classE enum class
     * @param n enum id
     * @param <E> type of enum class
     * @return enum that match provided id
     */
    public static <E extends Enum<E>> E get(Class<E> classE, Integer n) {
        Object t = null;
        try {
            t = classE.getMethod("values", (Class[]) null).invoke(classE, (Object[]) null);
        } catch (Exception e) {
        }
        for (E eItem : ((E[]) t)) {
            if (eItem.ordinal() == n)
                return eItem;
        }
        return ((E[]) t)[0];
    }

    /**
     * Returns enum that match provided {@link String} value
     * @param classE enum class
     * @param value {@link String} enum value
     * @param <E> type of enum class
     * @return enum that match provided {@link String} value
     */
    public static <E extends Enum<E>> E valueOf(Class<E> classE, String value) {
        Object t = null;
        try {
            t = classE.getMethod("values", (Class[]) null).invoke(classE, (Object[]) null);
            E e = (E) classE.getMethod("valueOf", String.class).invoke(classE, value);
            return e;
        } catch (Exception e) {
        }
        return ((E[]) t)[0];
    }

    /**
     * Returns enum that match provided {@link String} value or null
     * @param classE enum class
     * @param value {@link String} enum value
     * @param <E> type of enum class
     * @return enum that match provided {@link String} value or null
     */
    public static <E extends Enum<E>> E valueOfOrNull(Class<E> classE, String value) {
        Object t = null;
        try {
            t = classE.getMethod("values", (Class[]) null).invoke(classE, (Object[]) null);
            E e = (E) classE.getMethod("valueOf", String.class).invoke(classE, value);
            return e;
        } catch (Exception e) {
        }
        return null;
    }
}
