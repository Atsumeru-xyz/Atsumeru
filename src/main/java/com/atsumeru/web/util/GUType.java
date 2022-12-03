package com.atsumeru.web.util;

import java.io.*;
import java.util.Date;

public class GUType {

    public static boolean isTrailingSignificant(Float fFloat) {
        return ((fFloat - fFloat.intValue()) > 0);
    }

    /**
     * Safely returns error message from Throwable using {@link #getErrorMessageSafe(Throwable, boolean) getErrorMessageSafe}
     * but without Stacktrace
     * @param throwable - nullable throwable
     * @return - error message without stacktrace
     */
    public static String getErrorMessageSafe(Throwable throwable) {
        return getErrorMessageSafe(throwable, false);
    }

    /**
     * Safely returns error message from Throwable. If throwable is null, "Exception is null!" will be returned.
     * If getMessage() from throwable returns null, then "Unknown error of class: " + throwable.toString() will
     * be returned.
     * If {@param isWithStacktracePrint} set to true Stracktrace will be printed into sdterr
     * @param throwable - nullable throwable
     * @param isWithStacktracePrint - print stacktrace into sdterr
     * @return - error message
     */
    public static String getErrorMessageSafe(Throwable throwable, boolean isWithStacktracePrint) {
        if (throwable == null) {
            return "Exception is null!";
        }
        String errStr = throwable.getLocalizedMessage();
        if (GUString.isEmpty(errStr)) {
            errStr = throwable.getMessage();
        }
        if (GUString.isEmpty(errStr)) {
            errStr = "Unknown error of class: " + throwable.toString();
        }
        if (isWithStacktracePrint) {
            throwable.printStackTrace();
        }
        return errStr;
    }

    /**
     * Safely returns error stacktrace from Throwable. If throwable is null, "Exception is null!" will be returned.
     * If stacktrace is null, "Stacktrace is null!" will be returned.
     * @param throwable - nullable throwable
     * @return - error message
     */
    public static String getErrorStacktraceSafe(Throwable throwable) {
        if (throwable == null) {
            return "Exception is null!";
        }
        if (throwable.getStackTrace().length == 0) {
            return "Stacktrace is null!";
        }

        Writer result = new StringWriter();
        PrintWriter stacktrace = new PrintWriter(result);
        throwable.printStackTrace(stacktrace);
        return result.toString();
    }

    /**
     * Safelly parses Long from String. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     * @param value - input string for parsing
     * @param def - default return value
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
     * @param value - input string for parsing
     * @param def - default return value
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
     * @param value - input string for parsing
     * @param def - default return value
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
     * @param value - input string for parsing
     * @param def - default return value
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
     * @param value - input string for parsing
     * @param def - default return value
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

    /**
     * Safelly parses Date from Long. If provided {@param value} can't be parsed, provided default {@param def}
     * value will be returned.
     * @param value - input string for parsing
     * @param def - default return value
     * @return parsed Date or {@param def} if {@param value} is null or can't be parsed
     */
    public static Date getDateDef(Long value, Date def) {
        if (value == null) {
            return def;
        }
        try {
            return new Date(value);
        } catch (NumberFormatException var3) {
            return def;
        }
    }

    /**
     * Checks consistency of two objects
     * @param obj1 first object
     * @param obj2 second object
     * @return 0 if both objects is null or equals, -1 if obj1 is null, 1 if obj2 is null, null if objects not null
     * and not equals
     */
    public static Integer checkConsistentCompare(Object obj1, Object obj2) {
        Integer check = checkConsistentCompareOnly(obj1, obj2);
        if (check != null) {
            return check;
        }
        if (obj1.equals(obj2)) {
            return 0;
        }
        return null;
    }

    /**
     * Checks consistency of two objects
     * @param obj1 first object
     * @param obj2 second object
     * @return 0 if both objects is null, -1 if obj1 is null, 1 if obj2 is null, null if objects not null
     */
    public static Integer checkConsistentCompareOnly(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return 0;
        if (obj1 == null) return -1;
        if (obj2 == null) return 1;
        return null;
    }

    /**
     * Clones serializable object
     * @param serializable serializable object to clone
     * @param <T> type of serializable
     * @return cloned object
     */
    public static <T> T clone(Serializable serializable) {
        T cloned = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(serializable);
            oos.flush();
            oos.close();
            bos.close();
            byte[] byteData = bos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            cloned = (T) new ObjectInputStream(bais).readObject();
        } catch (Exception ex) {
            System.err.println(GUType.getErrorMessageSafe(ex));
        }
        return cloned;
    }

    /**
     * Get value depending of object type. Supports String, Integer, Long, Boolean.
     * @param value value that will be casted to input type
     * @param defValue input type
     * @param <F> type of object
     * @return value casted to object type
     */
    public static <F> F getTypeDef(String value, F defValue) {
        Object result = defValue;
        if (defValue instanceof String) {
            result = GUString.isEmpty(value) ? defValue : value;
        } else if (defValue instanceof Integer) {
            result = GUType.getIntDef(value, (Integer) defValue);
        } else if (defValue instanceof Long) {
            result = GUType.getLongDef(value, (Long) defValue);
        } else if (defValue instanceof Boolean) {
            result = GUType.getBoolDef(value, (Boolean) defValue);
        } else {
            throw new ClassCastException("Not realised!");
        }
        return (F) result;
    }
}
