package com.atsumeru.web.converter;

import com.atsumeru.web.util.EnumUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;

public class StringToNullableEnumConverter<E extends Enum<E>> implements Converter<String, E> {
    private final Class<E> clazz;

    public StringToNullableEnumConverter(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public E convert(@NotNull String source) {
        return EnumUtils.valueOfOrNull(clazz, EnumUtils.convertHumanizedToEnumName(source));
    }
}