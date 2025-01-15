package xyz.atsumeru.web.configuration;

import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.atsumeru.web.json.adapter.OmitEmptyStringsAdapter;

@Configuration
public class GsonConfiguration {

    @Bean
    public GsonBuilderCustomizer typeAdapterRegistration() {
        return builder -> builder.registerTypeAdapter(String.class, new OmitEmptyStringsAdapter());
    }
}