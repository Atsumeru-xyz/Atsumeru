package com.atsumeru.web.json.adapter;

import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.StringUtils;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StringListBidirectionalAdapter implements JsonSerializer<String>, JsonDeserializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        if (StringUtils.isEmpty(src)) {
            return null;
        }

        String[] array = src.split(",");
        if (ArrayUtils.isEmpty(array)) {
            return null;
        }

        JsonArray jsonArray = new JsonArray();
        for (String value : array) {
            jsonArray.add(value);
        }

        return jsonArray;
    }

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> list = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(it -> list.add(it.getAsString()));
        }

        return StringUtils.join(",", list);
    }
}
