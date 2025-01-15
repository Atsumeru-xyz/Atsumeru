package xyz.atsumeru.web.json.adapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import xyz.atsumeru.web.util.StringUtils;

import java.lang.reflect.Type;

public class OmitEmptyStringsAdapter implements JsonSerializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        return StringUtils.isNotEmpty(src) ? new JsonPrimitive(src) : null;
    }
}