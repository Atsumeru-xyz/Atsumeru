package com.atsumeru.web.helper;

import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUString;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

public class JSONLogHelper {

    public static void putJSON(JSONObject obj, String name, Collection<?> collection) throws JSONException {
        if (GUArray.isNotEmpty(collection)) {
            obj.put(name, collection);
        }
    }

    public static <E extends Enum<E>> void putJSON(JSONObject obj, String name, E e) throws JSONException {
        if (e != null) {
            obj.put(name, e.toString());
        }
    }

    public static void putJSON(JSONObject obj, String name, String value) throws JSONException {
        if (!GUString.isEmpty(value)) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, int value) throws JSONException {
        if (value > 0) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, long value) throws JSONException {
        if (value > 0) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, float value) throws JSONException {
        if (value > 0) {
            obj.put(name, value);
        }
    }

    public static void putJSON(JSONObject obj, String name, boolean value) throws JSONException {
        obj.put(name, value);
    }
}
