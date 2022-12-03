package com.atsumeru.web.json.adapter;

import com.atsumeru.web.util.GUString;
import com.google.gson.*;

import java.lang.reflect.Type;

public class OmitEmptyStringsAdapter implements JsonSerializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        return GUString.isNotEmpty(src) ? new JsonPrimitive(src) : null;
    }
}