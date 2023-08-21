package com.atsumeru.web.helper;

import com.atsumeru.web.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JSONHelper {

    public static JSONObject fromString(String string) {
        if (string == null) {
            return null;
        }
        JSONParser parser = new JSONParser();
        try {
            parser.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONObject(string);
    }

    /**
     * @param string
     * @return JSONArray or JSONObject
     * @throws JSONException
     */
    public static Object objectFromString(String string) throws JSONException {
        if (string.startsWith("{"))
            return new JSONObject(string);
        else
            return new JSONArray(string);
    }

    public static String getStringFromFile(final File f) throws IOException {
        if (!f.exists() || f.isDirectory()) {
            return null;
        }
        final StringBuilder str = new StringBuilder();
        final FileInputStream fin = new FileInputStream(f);
        final byte[] buff = new byte[1024];
        int n;
        while ((n = fin.read(buff)) > 0) {
            str.append(new String(buff, 0, n, StandardCharsets.UTF_8));
        }
        fin.close();
        return str.toString();
    }

    public static JSONObject fromFile(File file) throws IOException {
        final String s = getStringFromFile(file);
        if (s == null) {
            return null;
        }
        final JSONParser p = new JSONParser();
        try {
            p.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return new JSONObject(s);
    }

    public static JSONArray fromFileArray(final File f) throws IOException {
        final String s = getStringFromFile(f);
        if (s == null) {
            return null;
        }
        return new JSONArray(s);
    }

    public static void putJSON(JSONObject obj, String name, String value) throws JSONException {
        if (!StringUtils.isEmpty(value)) {
            obj.put(name, value);
        }
    }

    public static String getStringSafe(final JSONObject obj, final String name, final String def) throws JSONException {
        if (obj.has(name)) {
            return obj.getString(name);
        }
        return def;
    }

    public static String getStringSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getString(name);
        }
        return null;
    }

    public static String getStringSafe(final JSONObject obj, final String def, final String... names) throws JSONException {
        for (String name : names) {
            if (obj.has(name)) {
                return obj.getString(name);
            }
        }
        return def;
    }

    public static int getIntSafe(final JSONObject obj, final String name, final int def) {
        try {
            if (obj.has(name)) {
                return obj.getInt(name);
            }
        } catch (JSONException ignored) {
        }
        return def;
    }

    public static Float getFloatSafe(final JSONObject obj, final String name, final Float def) {
        try {
            if (obj.has(name)) {
                return (float) obj.getDouble(name);
            }
        } catch (JSONException ignored) {
        }
        return def;
    }

    public static boolean getBooleanSafe(final JSONObject obj, final String name, final boolean def) throws JSONException {
        if (obj.has(name)) {
            return obj.getBoolean(name);
        }
        return def;
    }

    public static Object getSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.get(name);
        }
        return null;
    }

    public static JSONObject getObjectSafe(JSONObject obj, String... names) throws JSONException {
        JSONObject o = null;
        for (String name : names) {
            o = getObjectSafe(obj, name);

            if (o != null) {
                break;
            }
        }
        return o;
    }

    public static JSONObject getObjectSafe(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONObject(name);
        }
        return null;
    }

    public static JSONObject getObjectSafe(final JSONObject obj, final String name, final JSONObject def) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONObject(name);
        }
        return def;
    }

    public static JSONArray getArraySafe(JSONObject obj, String... names) throws JSONException {
        JSONArray array = null;
        for (String name : names) {
            array = getArraySafe(obj, name);

            if (array != null) {
                break;
            }
        }
        return array;
    }

    public static JSONArray getArraySafe(JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getJSONArray(name);
        }
        return null;
    }

    public static HashMap<String, String> getMapSafe(JSONObject jsonObj, String name) {
        HashMap<String, String> jsonMap = null;
        try {
            if (jsonObj.has(name)) {
                Object obj1 = jsonObj.get(name);
                boolean isJSONObject = obj1 instanceof JSONObject;
                if (isJSONObject) {
                    jsonMap = new HashMap<>();
                    JSONObject obj2 = (JSONObject) obj1;
                    JSONArray obj3 = obj2.names();
                    int i = 0;
                    while (i < obj3.length()) {
                        String str = obj3.getString(i);
                        jsonMap.put(str, obj2.getString(str));
                        i = i + 1;
                    }
                }
            } else {
                jsonMap = null;
            }
        } catch (Exception ignored) {
        }
        return jsonMap;
    }

    public static HashMap<String, Object> getMapSafe(JSONObject object) {
        if (object == null) {
            return new HashMap<>();
        }
        return toMap(object);
    }

    public static HashMap<String, Object> toMap(JSONObject object) {
        HashMap<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

//            if (value instanceof JSONArray) {
//                value = toList((JSONArray) value);
//            }
//            else if(value instanceof JSONObject) {
//                value = toMap((JSONObject) value);
//            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static TreeMap<String, String> getMapSafeFromArray(JSONObject jsonObj, String arrayName, String nameAndValue,
                                                              String name, String value, boolean reverseOrder, boolean toLowerCaseKey) {
        TreeMap<String, String> jsonMap = null;
        try {
            if (jsonObj.has(arrayName)) {
                Object obj1 = jsonObj.get(arrayName);
                boolean isJSONArray = obj1 instanceof JSONArray;
                if (isJSONArray) {
                    jsonMap = new TreeMap<>(reverseOrder ? Collections.reverseOrder() : null);
                    for (Object obj : ((JSONArray) obj1)) {
                        if (obj instanceof JSONObject) {
                            JSONObject object = (JSONObject) obj;
                            String nameAndValueStr = JSONHelper.getStringSafe(object, nameAndValue);
                            String nameStr = JSONHelper.getStringSafe(object, name);
                            String valueStr = JSONHelper.getStringSafe(object, value);
                            if (nameAndValueStr != null) {
                                jsonMap.put(toLowerCaseKey ? nameAndValueStr.toLowerCase() : nameAndValueStr, nameAndValueStr);
                            } else if (nameStr != null && valueStr != null) {
                                jsonMap.put(toLowerCaseKey ? nameStr.toLowerCase() : nameStr, valueStr);
                            }
                        }
                    }
                }
            } else {
                jsonMap = null;
            }
        } catch (Exception ignored) {
        }
        return jsonMap;
    }

    public static long getLongSafe(final JSONObject obj, final String name, final long def) throws JSONException {
        if (obj.has(name)) {
            return obj.getLong(name);
        }
        return def;
    }

    public static Object get(final JSONObject obj, final String name) throws JSONException {
        if (obj.has(name)) {
            return obj.get(name);
        }
        return null;
    }

    public static String[] getStringArray(JSONObject obj, String name) throws JSONException {
        if (!obj.has(name)) {
            return null;
        }
        final Object o = get(obj, name);
        if (o instanceof String) {
            return new String[]{(String) o};
        }
        if (o instanceof JSONArray) {
            final JSONArray arr = (JSONArray) o;
            final String[] strings = new String[arr.length()];
            for (int i = 0; i < arr.length(); ++i) {
                strings[i] = arr.getString(i);
            }
            return strings;
        }
        return null;
    }

    public static List<String> getStringList(JSONObject obj, String name) throws JSONException {
        List<String> list = new ArrayList<>();
        if (!obj.has(name)) {
            return list;
        }
        Object o = get(obj, name);
        if (o instanceof String) {
            list.add((String) o);
            return list;
        }
        if (o instanceof JSONArray) {
            JSONArray arr = (JSONArray) o;
            for (int i = 0; i < arr.length(); ++i) {
                list.add(arr.getString(i));
            }
            return list;
        }
        return list;
    }
}
