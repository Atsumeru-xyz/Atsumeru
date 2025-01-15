package xyz.atsumeru.web.converter;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import xyz.atsumeru.web.util.EnumUtils;

public class StringToNonNullEnumConverter<E extends Enum<E>> implements Converter<String, E> {
    private final Class<E> clazz;

    public StringToNonNullEnumConverter(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public E convert(@NotNull String source) {
        return EnumUtils.valueOf(clazz, EnumUtils.convertHumanizedToEnumName(source));
    }
}