package xyz.atsumeru.web;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import xyz.atsumeru.web.json.annotation.Exclude;
import xyz.atsumeru.web.repository.dao.BooksDaoManager;

@Component
public class Beans {
    @Getter
    private static BooksDaoManager booksDaoManager;

    public Beans(BooksDaoManager booksDaoManager) {
        Beans.booksDaoManager = booksDaoManager;
    }

    // Custom beans
    @Profile("!dev")
    @Bean
    public Gson gson(ExclusionStrategy gsonExclusionStrategy) {
        return new Gson().newBuilder()
                .setExclusionStrategies(gsonExclusionStrategy)
                .create();
    }

    @Profile("dev")
    @Bean
    public Gson gsonPretty(ExclusionStrategy gsonExclusionStrategy) {
        return new Gson().newBuilder()
                .setExclusionStrategies(gsonExclusionStrategy)
                .setPrettyPrinting()
                .create();
    }

    @Bean
    public ExclusionStrategy gsonExclusionStrategy() {
        return new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getAnnotation(Exclude.class) != null;
            }
        };
    }
}
