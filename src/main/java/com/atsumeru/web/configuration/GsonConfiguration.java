package com.atsumeru.web.configuration;

import com.atsumeru.web.json.adapter.OmitEmptyStringsAdapter;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsonConfiguration {

    @Bean
    public GsonBuilderCustomizer typeAdapterRegistration() {
        return builder -> {
            builder.registerTypeAdapter(String.class, new OmitEmptyStringsAdapter());
        };
    }
}