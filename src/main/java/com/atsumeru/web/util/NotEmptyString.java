package com.atsumeru.web.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class NotEmptyString {
    private static final NotEmptyString EMPTY = new NotEmptyString();
    private String value;

    private NotEmptyString() {
        this.value = null;
    }

    private NotEmptyString(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static NotEmptyString of(String value) {
        return new NotEmptyString(value);
    }

    public static NotEmptyString ofNullable(String value) {
        return value == null ? EMPTY : of(value);
    }

    public String get() {
        if (GUString.isEmpty(value)) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public NotEmptyString apply(Function<String, String> function) {
        if (GUString.isNotEmpty(value)) {
            value = function.apply(value);
        }
        return this;
    }

    public boolean isPresent() {
        return GUString.isNotEmpty(value);
    }

    public String orElse(String other) {
        return GUString.isNotEmpty(value) ? value : other;
    }

    public String orElseGet(Supplier<String> other) {
        return GUString.isNotEmpty(value) ? value : other.get();
    }

    public <X extends Throwable> String orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (GUString.isNotEmpty(value)) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }
}
