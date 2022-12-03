package com.atsumeru.web.json.adapter;

import com.atsumeru.web.service.UserDatabaseDetailsService;
import com.google.gson.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Type;

public class AdminFieldAdapter implements JsonSerializer<String>, JsonDeserializer<String> {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UserDatabaseDetailsService.isUserInRole(auth, "ADMIN") ? context.serialize(src) : null;
    }

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return null;
    }
}