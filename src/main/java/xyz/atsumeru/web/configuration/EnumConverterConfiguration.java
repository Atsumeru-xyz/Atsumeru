package xyz.atsumeru.web.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.atsumeru.web.converter.StringToNonNullEnumConverter;
import xyz.atsumeru.web.converter.StringToNullableEnumConverter;
import xyz.atsumeru.web.enums.*;
import xyz.atsumeru.web.manager.ImageCache;

@Configuration
public class EnumConverterConfiguration implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Sort.class, new StringToNullableEnumConverter<>(Sort.class));
        registry.addConverter(String.class, ContentType.class, new StringToNullableEnumConverter<>(ContentType.class));
        registry.addConverter(String.class, Status.class, new StringToNullableEnumConverter<>(Status.class));
        registry.addConverter(String.class, TranslationStatus.class, new StringToNullableEnumConverter<>(TranslationStatus.class));
        registry.addConverter(String.class, PlotType.class, new StringToNullableEnumConverter<>(PlotType.class));
        registry.addConverter(String.class, Censorship.class, new StringToNullableEnumConverter<>(Censorship.class));
        registry.addConverter(String.class, AgeRating.class, new StringToNullableEnumConverter<>(AgeRating.class));
        registry.addConverter(String.class, ServiceType.class, new StringToNullableEnumConverter<>(ServiceType.class));

        registry.addConverter(String.class, LibraryPresentation.class, new StringToNonNullEnumConverter<>(LibraryPresentation.class));
        registry.addConverter(String.class, LogicalMode.class, new StringToNonNullEnumConverter<>(LogicalMode.class));
        registry.addConverter(String.class, ImageCache.ImageCacheType.class, new StringToNonNullEnumConverter<>(ImageCache.ImageCacheType.class));
    }
}