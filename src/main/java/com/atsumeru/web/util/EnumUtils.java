package com.atsumeru.web.util;

import java.util.Arrays;

public class EnumUtils {
    private static final String ENUM_NAME_PATTERN = "^.|.$";

    /**
     * Converts human readable enum name to allowed enum name replacing spaces to _
     *
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
     *
     * @param e enum class
     * @return {@link String[]} of all enum names
     */
    public static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.toString(e.getEnumConstants()).replaceAll(ENUM_NAME_PATTERN, "").split(", ");
    }

    /**
     * Returns Enum by id
     *
     * @param e   enum
     * @param n   enum id
     * @param <E> type of enum class
     * @return enum that match provided id
     */
    public static <E extends Enum<E>> E get(E e, Integer n) {
        E[] values = e.getDeclaringClass().getEnumConstants();
        for (E value : values) {
            if (value.ordinal() == n) {
                return value;
            }
        }
        return values[0];
    }

    /**
     * Returns Enum by id
     *
     * @param classE enum class
     * @param n      enum id
     * @param <E>    type of enum class
     * @return enum that match provided id
     */
    public static <E extends Enum<E>> E get(Class<E> classE, Integer n) {
        E[] values = classE.getEnumConstants();
        for (E value : values) {
            if (value.ordinal() == n) {
                return value;
            }
        }
        return values[0];
    }

    /**
     * Returns enum that match provided {@link String} value
     *
     * @param classE enum class
     * @param name   {@link String} enum name
     * @param <E>    type of enum class
     * @return enum that match provided {@link String} value
     */
    public static <E extends Enum<E>> E valueOf(Class<E> classE, String name) {
        return valueOf(classE, name, classE.getEnumConstants()[0]);
    }

    public static <E extends Enum<E>> E valueOf(Class<E> classE, String name, E def) {
        E[] values = classE.getEnumConstants();
        for (E value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return def;
    }

    /**
     * Returns enum that match provided {@link String} value or null
     *
     * @param classE enum class
     * @param name   {@link String} enum value
     * @param <E>    type of enum class
     * @return enum that match provided {@link String} value or null
     */
    public static <E extends Enum<E>> E valueOfOrNull(Class<E> classE, String name) {
        for (E value : classE.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
