package xyz.atsumeru.web.json.adapter;

import com.google.gson.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.atsumeru.web.security.service.UsersDetailsService;

import java.lang.reflect.Type;

public class AdminFieldAdapter implements JsonSerializer<String>, JsonDeserializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UsersDetailsService.isUserInRole(auth, "ADMIN") ? context.serialize(src) : null;
    }

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return null;
    }
}