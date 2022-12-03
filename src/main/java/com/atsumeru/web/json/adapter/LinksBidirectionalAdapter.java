package com.atsumeru.web.json.adapter;

import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GULinks;
import com.atsumeru.web.util.GUString;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinksBidirectionalAdapter implements JsonSerializer<String>, JsonDeserializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        if (GUString.isEmpty(src)) {
            return null;
        }

        String[] array = src.split(",");
        if (GUArray.isEmpty(array)) {
            return null;
        }

        JsonArray jsonArray = new JsonArray();

        Arrays.stream(array)
                .filter(GUString::isNotEmpty)
                .forEach(link -> {
                    JsonObject links = new JsonObject();
                    links.addProperty("source", GULinks.getHostName(link));
                    links.addProperty("link", link);
                    jsonArray.add(links);
                });

        return jsonArray;
    }

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> list = new ArrayList<>();
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(it -> list.add(it.getAsJsonObject().get("link").getAsString()));
        }

        return GUString.join(",", list);
    }
}
