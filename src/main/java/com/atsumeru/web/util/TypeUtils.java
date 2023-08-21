package com.atsumeru.web.util;

public class TypeUtils {

    public static boolean isTrailingSignificant(Float fFloat) {
        return ((fFloat - fFloat.intValue()) > 0);
    }

    /**
     * Safelly parses Long from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     *
     * @param value - input string for parsing
     * @param def   - default return value
     * @return parsed Long or {@param def} if {@param value} is null or can't be parsed
     */
    public static long getLongDef(String value, long def) {
        if (value == null) {
            return def;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Safelly parses Double from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     *
     * @param value - input string for parsing
     * @param def   - default return value
     * @return parsed Double or {@param def} if {@param value} is null or can't be parsed
     */
    public static double getDoubleDef(String value, long def) {
        if (value == null) {
            return def;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Safelly parses Float from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     *
     * @param value - input string for parsing
     * @param def   - default return value
     * @return parsed Float or {@param def} if {@param value} is null or can't be parsed
     */
    public static float getFloatDef(String value, float def) {
        if (value == null) {
            return def;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Safelly parses Integer from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     *
     * @param value - input string for parsing
     * @param def   - default return value
     * @return parsed Integer or {@param def} if {@param value} is null or can't be parsed
     */
    public static int getIntDef(String value, int def) {
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Safelly parses Boolean from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     *
     * @param value - input string for parsing
     * @param def   - default return value
     * @return parsed Boolean or {@param def} if {@param value} is null or can't be parsed
     */
    public static boolean getBoolDef(String value, boolean def) {
        if (value == null) {
            return def;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
