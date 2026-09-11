package com.transportlogistics.app.tracking.application.provider;

import java.io.Serial;
import java.io.Serializable;
import java.util.regex.Pattern;

public record ProviderType(String value) implements Comparable<ProviderType>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Pattern VALID = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

    public ProviderType {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Provider type must match [A-Z][A-Z0-9_]{0,63}");
        }
    }

    public static ProviderType of(String value) {
        return new ProviderType(value);
    }

    @Override
    public int compareTo(ProviderType other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
