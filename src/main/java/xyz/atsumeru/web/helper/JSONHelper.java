package xyz.atsumeru.web.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xyz.atsumeru.web.util.StringUtils;

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

    public static String getStringFromFile(File file) throws IOException {
        if (!file.exists() || file.isDirectory()) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        FileInputStream fin = new FileInputStream(file);
        byte[] buff = new byte[1024];
        int n;
        while ((n = fin.read(buff)) > 0) {
            str.append(new String(buff, 0, n, StandardCharsets.UTF_8));
        }
        fin.close();
        return str.toString();
    }

    public static JSONObject fromFile(File file) throws IOException {
        return fromString(getStringFromFile(file));
    }

    public static JSONArray fromFileArray(File file) throws IOException {
        return Optional.ofNullable(getStringFromFile(file))
                .map(JSONArray::new)
                .orElse(null);
    }

    public static void putJSON(JSONObject jsonObject, String name, String value) throws JSONException {
        if (!StringUtils.isEmpty(value)) {
            jsonObject.put(name, value);
        }
    }

    public static String getStringSafe(JSONObject jsonObject, String name, String def) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getString(name);
        }
        return def;
    }

    public static String getStringSafe(JSONObject jsonObject, String name) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getString(name);
        }
        return null;
    }

    public static String getStringSafe(JSONObject jsonObject, String def, String... names) throws JSONException {
        for (String name : names) {
            if (jsonObject.has(name)) {
                return jsonObject.getString(name);
            }
        }
        return def;
    }

    public static int getIntSafe(JSONObject jsonObject, String name, int def) {
        try {
            if (jsonObject.has(name)) {
                return jsonObject.getInt(name);
            }
        } catch (JSONException ignored) {
        }
        return def;
    }

    public static Float getFloatSafe(JSONObject jsonObject, String name, Float def) {
        try {
            if (jsonObject.has(name)) {
                return (float) jsonObject.getDouble(name);
            }
        } catch (JSONException ignored) {
        }
        return def;
    }

    public static boolean getBooleanSafe(JSONObject jsonObject, String name, boolean def) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getBoolean(name);
        }
        return def;
    }

    public static Object getSafe(JSONObject jsonObject, String name) throws JSONException {
        return get(jsonObject, name);
    }

    public static JSONObject getObjectSafe(JSONObject jsonObject, String... names) throws JSONException {
        JSONObject o = null;
        for (String name : names) {
            o = getObjectSafe(jsonObject, name);

            if (o != null) {
                break;
            }
        }
        return o;
    }

    public static JSONObject getObjectSafe(JSONObject jsonObject, String name) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getJSONObject(name);
        }
        return null;
    }

    public static JSONObject getObjectSafe(JSONObject jsonObject, String name, JSONObject def) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getJSONObject(name);
        }
        return def;
    }

    public static JSONArray getArraySafe(JSONObject jsonObject, String... names) throws JSONException {
        JSONArray array = null;
        for (String name : names) {
            array = getArraySafe(jsonObject, name);

            if (array != null) {
                break;
            }
        }
        return array;
    }

    public static JSONArray getArraySafe(JSONObject jsonObject, String name) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getJSONArray(name);
        }
        return null;
    }

    public static HashMap<String, String> getMapSafe(JSONObject jsonObj, String name) {
        HashMap<String, String> jsonMap = null;
        try {
            if (jsonObj.has(name)) {
                Object obj = jsonObj.get(name);
                if (obj instanceof JSONObject jsonObject) {
                    jsonMap = new HashMap<>();
                    JSONArray jsonArray = jsonObject.names();
                    int i = 0;
                    while (i < jsonArray.length()) {
                        String str = jsonArray.getString(i);
                        jsonMap.put(str, jsonObject.getString(str));
                        i = i + 1;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return jsonMap;
    }

    public static HashMap<String, Object> getMapSafe(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new HashMap<>();
        }
        return toMap(jsonObject);
    }

    public static HashMap<String, Object> toMap(JSONObject jsonObject) {
        HashMap<String, Object> map = new HashMap<>();
        Iterator<String> keysItr = jsonObject.keys();

        while (keysItr.hasNext()) {
            String key = keysItr.next();
            map.put(key, jsonObject.get(key));
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray jsonArray) {
                value = toList(jsonArray);
            } else if (value instanceof JSONObject jsonObject) {
                value = toMap(jsonObject);
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
                if (jsonObj.get(arrayName) instanceof JSONArray jsonArray) {
                    jsonMap = new TreeMap<>(reverseOrder ? Collections.reverseOrder() : null);
                    for (Object obj : jsonArray) {
                        if (obj instanceof JSONObject jsonObject) {
                            String nameAndValueStr = JSONHelper.getStringSafe(jsonObject, nameAndValue);
                            String nameStr = JSONHelper.getStringSafe(jsonObject, name);
                            String valueStr = JSONHelper.getStringSafe(jsonObject, value);
                            if (nameAndValueStr != null) {
                                jsonMap.put(toLowerCaseKey ? nameAndValueStr.toLowerCase() : nameAndValueStr, nameAndValueStr);
                            } else if (nameStr != null && valueStr != null) {
                                jsonMap.put(toLowerCaseKey ? nameStr.toLowerCase() : nameStr, valueStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return jsonMap;
    }

    public static long getLongSafe(JSONObject jsonObject, String name, long def) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getLong(name);
        }
        return def;
    }

    public static Object get(JSONObject jsonObject, String name) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.get(name);
        }
        return null;
    }

    public static String[] getStringArray(JSONObject jsonObject, String name) throws JSONException {
        if (!jsonObject.has(name)) {
            return null;
        }
        Object obj = get(jsonObject, name);
        if (obj instanceof String str) {
            return new String[]{str};
        }
        if (obj instanceof JSONArray arr) {
            String[] strings = new String[arr.length()];
            for (int i = 0; i < arr.length(); ++i) {
                strings[i] = arr.getString(i);
            }
            return strings;
        }
        return null;
    }

    public static List<String> getStringList(JSONObject jsonObject, String name) throws JSONException {
        List<String> list = new ArrayList<>();
        if (!jsonObject.has(name)) {
            return list;
        }
        Object obj = get(jsonObject, name);
        if (obj instanceof String str) {
            list.add(str);
            return list;
        }
        if (obj instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.length(); ++i) {
                list.add(jsonArray.getString(i));
            }
            return list;
        }
        return list;
    }
}
